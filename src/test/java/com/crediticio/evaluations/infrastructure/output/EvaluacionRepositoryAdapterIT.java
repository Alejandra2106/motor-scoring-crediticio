package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.domain.ClasificacionEvaluacion;
import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.infrastructure.output.ReglaScoringRepositoryAdapter;
import com.crediticio.support.ProteccionBaseIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración de persistencia contra PostgreSQL local real (no H2), siguiendo el
 * mismo patrón que {@code ReglaScoringRepositoryAdapterIT} (HU05).
 *
 * Requiere PostgreSQL local y el perfil "it" (src/test/resources/application-it.properties).
 * No usa DB_URL: la URL se indica explícitamente con -D y {@link ProteccionBaseIntegracion}
 * aborta antes de crear el contexto (sin conectarse) si no apunta exactamente a la base
 * dedicada autorizada. DB_USERNAME y DB_PASSWORD se toman del entorno.
 *
 * HU12: la entidad de evaluación incluye score_total, categoria e id_politica, que existen desde la
 * migración V8 (src/main/resources/db/migration). El comando usa un esquema dedicado y temporal
 * (hu12_it) para no aplicar migraciones en el esquema public.
 *
 * Al terminar en "IT" (no "Test"), Surefire no la ejecuta con `./mvnw test`.
 * Ejecución manual (requiere autorización expresa; crea y usa el esquema hu12_it):
 * ./mvnw.cmd -B test "-Dtest=EvaluacionRepositoryAdapterIT" "-Dspring.profiles.active=it"
 *     "-Dit.base-dedicada=motor_scoring_it" "-Dit.esquema-dedicado=hu12_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it?currentSchema=hu12_it"
 *     "-Dspring.flyway.schemas=hu12_it"
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@ContextConfiguration(initializers = ProteccionBaseIntegracion.class)
@Import({EvaluacionRepositoryAdapter.class, ReglaScoringRepositoryAdapter.class})
class EvaluacionRepositoryAdapterIT {

    @Autowired
    private EvaluacionRepositoryAdapter evaluacionRepositoryAdapter;

    @Autowired
    private ReglaScoringRepositoryAdapter reglaScoringRepositoryAdapter;

    @Autowired
    private EvaluacionJpaRepository evaluacionJpaRepository;

    @Autowired
    private DetalleEvaluacionJpaRepository detalleEvaluacionJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void elContextoCargaConFlywayYLaValidacionDeEsquemaDeHibernate() {
        assertThat(evaluacionRepositoryAdapter).isNotNull();
        assertThat(evaluacionJpaRepository).isNotNull();
    }

    @Test
    void debeGuardarLaEvaluacionYGenerarIdYFechaDeEvaluacion() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        ReglaFixture regla = crearReglaFixture(idRiesgo, ">=", "3000000", 20);

        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante,
                List.of(DetalleEvaluacion.nuevo(regla.idRegla(), ">=", regla.valorCondicion(), 20, true, 20)),
                clasificacionAltaFixture());

        Evaluacion guardada = evaluacionRepositoryAdapter.guardar(evaluacion);

        assertThat(guardada.getIdEvaluacion()).isNotNull();
        assertThat(guardada.getIdSolicitante()).isEqualTo(idSolicitante);
        assertThat(guardada.getFechaEvaluacion()).isNotNull();
    }

    @Test
    void debePersistirTodosLosDetallesDeLaEvaluacionConSuSnapshot() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Long idRiesgo1 = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        Long idRiesgo2 = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NUMERO_MORAS);
        ReglaFixture regla1 = crearReglaFixture(idRiesgo1, ">=", "3000000", 20);
        ReglaFixture regla2 = crearReglaFixture(idRiesgo2, "=", "0", 10);

        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante, List.of(
                DetalleEvaluacion.nuevo(regla1.idRegla(), ">=", regla1.valorCondicion(), 20, true, 20),
                DetalleEvaluacion.nuevo(regla2.idRegla(), "=", regla2.valorCondicion(), 10, false, 0)),
                clasificacionAltaFixture());

        Evaluacion guardada = evaluacionRepositoryAdapter.guardar(evaluacion);

        List<DetalleEvaluacionJpaEntity> detallesPersistidos =
                detalleEvaluacionJpaRepository.findByIdEvaluacion(guardada.getIdEvaluacion());
        assertThat(detallesPersistidos).hasSize(2);
        assertThat(guardada.getScoreTotal()).isEqualTo(20);
        assertThat(guardada.getDetalles()).extracting(DetalleEvaluacion::getIdDetalleEvaluacion)
                .doesNotContainNull();
    }

    @Test
    void debePreservarElSnapshotHistoricoDespuesDeEditarLaReglaOriginal() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Long idRiesgo = obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO);
        ReglaFixture regla = crearReglaFixture(idRiesgo, "<=", "40", 10);

        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante,
                List.of(DetalleEvaluacion.nuevo(regla.idRegla(), "<=", regla.valorCondicion(), 10, true, 10)),
                clasificacionAltaFixture());
        Evaluacion guardada = evaluacionRepositoryAdapter.guardar(evaluacion);

        // La regla se edita después (mismo registro, HU06): el detalle histórico no debe cambiar.
        // Se edita a través del propio EntityManager de la prueba (no una conexión JDBC aparte):
        // el detalle recién insertado ya referencia esta fila de regla_scoring por FK, lo que
        // mantiene un lock FOR KEY SHARE sobre ella hasta que termine la transacción de la
        // prueba; una conexión distinta intentando el UPDATE se bloquearía indefinidamente
        // esperando ese lock.
        String valorEditadoUnico = "20." + System.nanoTime();
        ReglaScoring reglaExistente = reglaScoringRepositoryAdapter.buscarPorId(regla.idRegla()).orElseThrow();
        ReglaScoring reglaEditada = reglaExistente.editar(
                OperadorScoring.MENOR, valorEditadoUnico, 5, TipoVariable.NUMERICO, true);
        reglaScoringRepositoryAdapter.guardar(reglaEditada);

        DetalleEvaluacionJpaEntity detallePersistido =
                detalleEvaluacionJpaRepository.findByIdEvaluacion(guardada.getIdEvaluacion()).get(0);
        assertThat(detallePersistido.getOperadorAplicado()).isEqualTo("<=");
        assertThat(detallePersistido.getValorCondicionAplicado()).isEqualTo(regla.valorCondicion());
        assertThat(detallePersistido.getPuntajeReglaAplicado()).isEqualTo(10);
        assertThat(detallePersistido.getPuntajeObtenido()).isEqualTo(10);
    }

    @Test
    void laBaseDeDatosDebeRechazarUnDetalleConIdReglaInexistentePorLaClaveForanea() throws SQLException {
        Long idSolicitante = crearSolicitanteFixture();
        Evaluacion evaluacion = Evaluacion.nueva(idSolicitante,
                List.of(DetalleEvaluacion.nuevo(-1L, "=", "10", 5, true, 5)),
                clasificacionAltaFixture());

        assertThatThrownBy(() -> evaluacionRepositoryAdapter.guardar(evaluacion))
                .isInstanceOf(DataIntegrityViolationException.class);

        // La evaluación insertada antes del fallo del detalle no queda visible para otras
        // conexiones: la transacción completa no se confirma (RF15/atomicidad).
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM evaluacion WHERE id_solicitante = ?")) {
            statement.setLong(1, idSolicitante);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getLong(1)).isZero();
            }
        }
    }

    private Long crearSolicitanteFixture() throws SQLException {
        String numeroDocumento = "9" + (System.nanoTime() % 10_000_000_000_000L);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO solicitante (nombre_completo, numero_documento, ingresos_mensuales, "
                                + "deudas_mensuales, numero_moras, historial_crediticio, antiguedad_laboral) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_solicitante")) {
            statement.setString(1, "Solicitante de prueba HU07");
            statement.setString(2, numeroDocumento);
            statement.setBigDecimal(3, new BigDecimal("4000000"));
            statement.setBigDecimal(4, new BigDecimal("1200000"));
            statement.setInt(5, 0);
            statement.setString(6, "BUENO");
            statement.setBigDecimal(7, new BigDecimal("2"));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Long obtenerOAsegurarRiesgoActivo(NombreVariableRiesgo variable) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO riesgo (variable, descripcion, estado) VALUES (?, ?, 'ACTIVA') "
                                + "ON CONFLICT (variable) DO UPDATE SET estado = 'ACTIVA' RETURNING id_riesgo")) {
            statement.setString(1, variable.name());
            statement.setString(2, "Descripción válida con más de diez caracteres");
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private ReglaFixture crearReglaFixture(Long idRiesgo, String operador, String valorCondicion, int puntaje)
            throws SQLException {
        String valorUnico = valorCondicion + "." + System.nanoTime();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_regla")) {
            statement.setLong(1, idRiesgo);
            statement.setString(2, operador);
            statement.setString(3, valorUnico);
            statement.setInt(4, puntaje);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new ReglaFixture(resultSet.getLong(1), valorUnico);
            }
        }
    }

    /**
     * HU12: toda evaluación nueva referencia una política publicada. Inserta una versión nueva con
     * rangos ALTO [-100, 39], MEDIO [40, 79] y BAJO [80, 130]; los puntajes de estas pruebas (de 5 a
     * 20) corresponden a ALTO. No modifica la política vigente.
     */
    private ClasificacionEvaluacion clasificacionAltaFixture() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            long idPolitica;
            int version;
            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT COALESCE(MAX(version), 0) + 1 FROM politica_clasificacion")) {
                resultSet.next();
                version = resultSet.getInt(1);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO politica_clasificacion (version, publicado_por) VALUES (?, ?) RETURNING id_politica")) {
                statement.setInt(1, version);
                statement.setObject(2, UUID.randomUUID(), java.sql.Types.OTHER);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    idPolitica = resultSet.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, limite_superior) "
                            + "VALUES (?, 'ALTO', -100, 39), (?, 'MEDIO', 40, 79), (?, 'BAJO', 80, 130)")) {
                statement.setLong(1, idPolitica);
                statement.setLong(2, idPolitica);
                statement.setLong(3, idPolitica);
                statement.executeUpdate();
            }
            return new ClasificacionEvaluacion("ALTO", idPolitica, version);
        }
    }

    private record ReglaFixture(Long idRegla, String valorCondicion) {
    }
}
