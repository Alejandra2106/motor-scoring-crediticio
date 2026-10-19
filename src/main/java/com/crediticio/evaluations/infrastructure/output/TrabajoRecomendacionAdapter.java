package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.ResultadoReclamo;
import com.crediticio.evaluations.ports.output.TrabajoRecomendacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Outbox de la saga (HU14, V11). Cada registro es una transacción corta propia (nunca abierta durante una
 * llamada HTTP) que sigue el orden canónico de bloqueo: primero la evaluación ({@code FOR NO KEY UPDATE}) y
 * después el trabajo. Todo registro de resultado exige el token de la concesión vigente: si otro trabajador
 * reclamó el trabajo, no se modifica ninguna fila. Las horas salen de clock_timestamp() de la base de datos.
 * Ante un interbloqueo (40P01) o un lock_timeout (55P03), el registro se reintenta hasta tres veces.
 *
 * Las sentencias usan sintaxis de PostgreSQL; la creación del trabajo, que se ejecuta en la transacción de la
 * evaluación, usa JPA.
 */
@Component
public class TrabajoRecomendacionAdapter implements TrabajoRecomendacionPort {

    private static final Logger log = LoggerFactory.getLogger(TrabajoRecomendacionAdapter.class);
    private static final int INTENTOS_DE_REGISTRO = 3;
    private static final String CONCESIONES_AGOTADAS = "CONCESIONES_AGOTADAS";

    private static final String TIEMPOS_LOCALES = "SET LOCAL lock_timeout = '2s'";
    private static final String TIEMPO_SENTENCIA = "SET LOCAL statement_timeout = '5s'";
    private static final String BLOQUEAR_EVALUACION =
            "SELECT 1 FROM evaluacion WHERE id_evaluacion = ? FOR NO KEY UPDATE";
    private static final String SELECCIONAR_CANDIDATO = """
            SELECT id_evaluacion, intentos FROM trabajo_recomendacion
            WHERE estado_trabajo = 'PENDIENTE' AND proximo_intento <= clock_timestamp()
              AND (concesion_hasta IS NULL OR concesion_hasta < clock_timestamp())
            ORDER BY proximo_intento
            LIMIT 1
            FOR NO KEY UPDATE SKIP LOCKED
            """;
    private static final String RECLAMAR = """
            UPDATE trabajo_recomendacion
               SET id_concesion = ?, concesion_hasta = clock_timestamp() + make_interval(secs => ?),
                   intentos = intentos + 1, intentos_totales = intentos_totales + 1, actualizado_en = clock_timestamp()
             WHERE id_evaluacion = ?
            """;
    private static final String AGOTAR = """
            UPDATE trabajo_recomendacion
               SET estado_trabajo = 'ERROR_PERMANENTE', id_concesion = NULL, concesion_hasta = NULL,
                   codigo_ultimo_error = ?, ultimo_error = ?, actualizado_en = clock_timestamp()
             WHERE id_evaluacion = ? AND estado_trabajo = 'PENDIENTE' AND intentos >= ?
               AND (concesion_hasta IS NULL OR concesion_hasta < clock_timestamp())
            """;
    private static final String PROCESAR = """
            UPDATE trabajo_recomendacion
               SET estado_trabajo = 'PROCESADO', procesado_en = clock_timestamp(), id_concesion = NULL,
                   concesion_hasta = NULL, actualizado_en = clock_timestamp()
             WHERE id_evaluacion = ? AND id_concesion = ? AND estado_trabajo = 'PENDIENTE'
            """;
    private static final String COMPLETAR_EVALUACION =
            "UPDATE evaluacion SET estado = 'COMPLETADA' WHERE id_evaluacion = ? AND estado = 'PENDIENTE'";
    private static final String ERROR_TRANSITORIO = """
            UPDATE trabajo_recomendacion
               SET id_concesion = NULL, concesion_hasta = NULL,
                   proximo_intento = clock_timestamp() + make_interval(secs => ?),
                   codigo_ultimo_error = ?, ultimo_error = ?, actualizado_en = clock_timestamp()
             WHERE id_evaluacion = ? AND id_concesion = ? AND estado_trabajo = 'PENDIENTE'
            """;
    private static final String ERROR_PERMANENTE = """
            UPDATE trabajo_recomendacion
               SET estado_trabajo = 'ERROR_PERMANENTE', id_concesion = NULL, concesion_hasta = NULL,
                   codigo_ultimo_error = ?, ultimo_error = ?, actualizado_en = clock_timestamp()
             WHERE id_evaluacion = ? AND id_concesion = ? AND estado_trabajo = 'PENDIENTE'
            """;

    private final TrabajoRecomendacionJpaRepository repositorio;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaccionNueva;

    public TrabajoRecomendacionAdapter(TrabajoRecomendacionJpaRepository repositorio, JdbcTemplate jdbcTemplate,
            PlatformTransactionManager gestorTransacciones) {
        this.repositorio = repositorio;
        this.jdbcTemplate = jdbcTemplate;
        this.transaccionNueva = new TransactionTemplate(gestorTransacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** En la transacción de la evaluación (la del llamador). */
    @Override
    public void crear(long idEvaluacion, UUID token, Duration concesion) {
        Instant ahora = Instant.now();
        repositorio.saveAndFlush(TrabajoRecomendacionJpaEntity.conConcesionInicial(idEvaluacion, token, ahora,
                ahora.plus(concesion)));
    }

    @Override
    public ResultadoReclamo reclamarSiguiente(int maxIntentos, UUID token, Duration concesion) {
        ResultadoReclamo seleccion = transaccionNueva.execute(estado -> {
            limitarTiempos();
            List<long[]> candidatos = jdbcTemplate.query(SELECCIONAR_CANDIDATO,
                    (fila, numero) -> new long[] {fila.getLong("id_evaluacion"), fila.getInt("intentos")});
            if (candidatos.isEmpty()) {
                return new ResultadoReclamo.Ninguno();
            }
            long idEvaluacion = candidatos.get(0)[0];
            int intentos = (int) candidatos.get(0)[1];
            if (intentos >= maxIntentos) {
                return new ResultadoReclamo.Agotado(idEvaluacion);
            }
            jdbcTemplate.update(RECLAMAR, token, segundos(concesion), idEvaluacion);
            return new ResultadoReclamo.Reclamado(idEvaluacion, token, intentos + 1);
        });
        if (seleccion instanceof ResultadoReclamo.Agotado agotado) {
            // transacción aparte y en orden canónico: evaluación y después trabajo
            conReintentos(() -> transaccionNueva.execute(estado -> {
                limitarTiempos();
                jdbcTemplate.queryForList(BLOQUEAR_EVALUACION, agotado.idEvaluacion());
                return jdbcTemplate.update(AGOTAR, CONCESIONES_AGOTADAS,
                        "Se agotaron los intentos sin una concesión vigente", agotado.idEvaluacion(), maxIntentos) > 0;
            }));
        }
        return seleccion;
    }

    @Override
    public boolean registrarExito(long idEvaluacion, UUID token) {
        return conReintentos(() -> transaccionNueva.execute(estado -> {
            limitarTiempos();
            jdbcTemplate.queryForList(BLOQUEAR_EVALUACION, idEvaluacion);
            if (jdbcTemplate.update(PROCESAR, idEvaluacion, token) == 0
                    || jdbcTemplate.update(COMPLETAR_EVALUACION, idEvaluacion) == 0) {
                estado.setRollbackOnly();
                return false;
            }
            return true;
        }));
    }

    @Override
    public boolean registrarErrorTransitorio(long idEvaluacion, UUID token, Duration espera, String codigo,
            String detalle) {
        return conReintentos(() -> transaccionNueva.execute(estado -> {
            limitarTiempos();
            jdbcTemplate.queryForList(BLOQUEAR_EVALUACION, idEvaluacion);
            return jdbcTemplate.update(ERROR_TRANSITORIO, segundos(espera), truncar(codigo, 50),
                    truncar(detalle, 500), idEvaluacion, token) > 0;
        }));
    }

    @Override
    public boolean registrarErrorPermanente(long idEvaluacion, UUID token, String codigo, String detalle) {
        return conReintentos(() -> transaccionNueva.execute(estado -> {
            limitarTiempos();
            jdbcTemplate.queryForList(BLOQUEAR_EVALUACION, idEvaluacion);
            return jdbcTemplate.update(ERROR_PERMANENTE, truncar(codigo, 50), truncar(detalle, 500), idEvaluacion,
                    token) > 0;
        }));
    }

    private void limitarTiempos() {
        jdbcTemplate.execute(TIEMPOS_LOCALES);
        jdbcTemplate.execute(TIEMPO_SENTENCIA);
    }

    private static boolean conReintentos(Supplier<Boolean> registro) {
        for (int intento = 1; ; intento++) {
            try {
                return Boolean.TRUE.equals(registro.get());
            } catch (PessimisticLockingFailureException ex) {
                if (intento >= INTENTOS_DE_REGISTRO) {
                    throw ex;
                }
                log.warn("Registro de la saga reintentado tras un conflicto de bloqueo ({})", ex.getClass().getSimpleName());
            }
        }
    }

    private static double segundos(Duration duracion) {
        return duracion.toMillis() / 1000.0;
    }

    private static String truncar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
