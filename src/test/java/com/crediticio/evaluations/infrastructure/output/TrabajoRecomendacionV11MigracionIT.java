package com.crediticio.evaluations.infrastructure.output;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU14 (propuesta, no promovida): aplicación de la migración V11 de database/propuestas/hu14 (estado
 * de la evaluación y outbox trabajo_recomendacion de la saga) en un esquema aislado y temporal.
 *
 * Antes de abrir cualquier conexión exige, por igualdad exacta de texto, que -Dit.base-dedicada sea
 * motor_scoring_it y que -Dspring.datasource.url sea jdbc:postgresql://localhost:5432/motor_scoring_it
 * (nunca usa DB_URL). Después comprueba con current_database() que la conexión llega a esa base y
 * exige que el esquema no exista. Aplica V1-V7 e inserta una evaluación anterior a V8, aplica V8-V9 e
 * inserta una evaluación clasificada anterior a la saga, aplica V11 y verifica sus invariantes. No toca
 * el esquema public ni sus datos.
 *
 * Limpieza: solo elimina hu14_v11_it si la base demuestra que lo creó esta ejecución (mismo OID
 * registrado tras crearlo y exactamente una marca de creación de Flyway, con el historial bloqueado).
 * Si no, no elimina nada y hace fallar la ejecución indicando el motivo. DROP SCHEMA no dispara los
 * triggers de borrado ni de truncado de V11.
 *
 * El rol de la prueba es el propietario de las tablas (las crea su propia ejecución de Flyway), por lo
 * que la reactivación directa por un rol que no es el propietario no se puede probar aquí: exige un
 * segundo rol, cuya creación no está autorizada.
 *
 * Al terminar en "IT", Surefire no la ejecuta con `./mvnw test`. Ejecución manual (requiere
 * autorización expresa para usar motor_scoring_it), desde la raíz del proyecto:
 *   mvnw.cmd -B test "-Dtest=TrabajoRecomendacionV11MigracionIT"
 *     "-Dit.base-dedicada=motor_scoring_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
 * con DB_USERNAME y DB_PASSWORD en el entorno.
 */
class TrabajoRecomendacionV11MigracionIT {

    private static final String ESQUEMA = "hu14_v11_it";
    /** Únicos valores admitidos; se comparan por igualdad exacta antes de abrir cualquier conexión. */
    private static final String BASE_AUTORIZADA = "motor_scoring_it";
    private static final String URL_AUTORIZADA = "jdbc:postgresql://localhost:5432/" + BASE_AUTORIZADA;
    private static final String UBICACION_MIGRACIONES = "classpath:db/migration";
    private static final String UBICACION_PROPUESTA = "filesystem:database/propuestas/hu14";
    private static final String CHECK_VIOLATION = "23514";
    private static final String OBJECT_NOT_IN_PREREQUISITE_STATE = "55000";
    private static final String LOCK_NOT_AVAILABLE = "55P03";

    private static String url;
    private static String usuario;
    private static String clave;
    /** Solo habilita el search_path del esquema; la limpieza no se basa en esta marca. */
    private static boolean esquemaCreadoPorLaPrueba;
    /** OID del esquema leído justo después de que Flyway lo creara; null si no llegó a registrarse. */
    private static Long oidEsquemaCreado;

    private static long idSolicitante;
    private static long idPolitica;
    private static long idHistoricaAnteriorAV8;
    private static long idHistoricaPosteriorAV8;

    /** Evaluación PENDIENTE con su trabajo y el token de la concesión vigente. */
    private record Par(long id, UUID token) {
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar(Connection connection) throws SQLException;
    }

    @BeforeAll
    static void prepararEsquemaConHistoricasYAplicarV11() throws SQLException {
        String base = System.getProperty("it.base-dedicada");
        url = System.getProperty("spring.datasource.url");
        usuario = System.getenv("DB_USERNAME");
        clave = System.getenv("DB_PASSWORD");
        if (!BASE_AUTORIZADA.equals(base)) {
            throw new IllegalStateException("Debe indicarse -Dit.base-dedicada=" + BASE_AUTORIZADA);
        }
        if (!URL_AUTORIZADA.equals(url)) {
            throw new IllegalStateException("spring.datasource.url debe indicarse con -D y ser exactamente "
                    + URL_AUTORIZADA);
        }
        assertThat(usuario).as("DB_USERNAME").isNotBlank();
        assertThat(clave).as("DB_PASSWORD").isNotBlank();

        try (Connection connection = conectar();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT current_database(), EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getString(1)).as("base de la conexión").isEqualTo(base);
                assertThat(resultSet.getBoolean(2))
                        .as("el esquema " + ESQUEMA + " ya existe; no se reutiliza ni se elimina").isFalse();
            }
        }

        esquemaCreadoPorLaPrueba = true;
        flyway("7").migrate();
        oidEsquemaCreado = registrarEsquemaCreadoPorFlyway();
        try (Connection connection = conectar()) {
            idSolicitante = insertarDevolviendoId(connection,
                    "INSERT INTO solicitante (nombre_completo, numero_documento, ingresos_mensuales, deudas_mensuales, "
                            + "numero_moras, historial_crediticio, antiguedad_laboral) "
                            + "VALUES ('Solicitante HU14', '900000014', 4000000, 1200000, 0, 'BUENO', 2) "
                            + "RETURNING id_solicitante");
            idHistoricaAnteriorAV8 = insertarDevolviendoId(connection,
                    "INSERT INTO evaluacion (id_solicitante) VALUES (" + idSolicitante + ") RETURNING id_evaluacion");
            idPolitica = publicarPolitica(connection);
        }

        flyway("9").migrate();
        try (Connection connection = conectar()) {
            // puntaje 0 sin detalles (suma 0, trigger A de V8); 0 pertenece al rango BAJO (trigger B)
            idHistoricaPosteriorAV8 = insertarDevolviendoId(connection, "INSERT INTO evaluacion (id_solicitante, "
                    + "score_total, categoria, id_politica) VALUES (" + idSolicitante + ", 0, 'BAJO', " + idPolitica
                    + ") RETURNING id_evaluacion");
        }

        flyway("11").migrate();
    }

    @AfterAll
    static void eliminarSoloElEsquemaCreadoPorLaPrueba() {
        if (!esquemaCreadoPorLaPrueba) {
            return; // la prueba abortó antes de crear nada
        }
        String motivo;
        try (Connection connection = DriverManager.getConnection(url, usuario, clave)) {
            connection.setAutoCommit(false);
            try {
                motivo = motivoParaNoEliminarElEsquema(connection);
                if (motivo == null) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("DROP SCHEMA " + ESQUEMA + " CASCADE");
                    }
                    connection.commit();
                    return;
                }
                connection.rollback();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException falloAlRevertir) {
                    ex.addSuppressed(falloAlRevertir);
                }
                throw ex;
            }
        } catch (SQLException ex) {
            motivo = "falló la comprobación o el DROP; transacción revertida (SQLState " + ex.getSQLState() + ")";
        }
        throw new AssertionError("No se eliminó el esquema " + ESQUEMA + ": " + motivo
                + ". Revíselo manualmente antes de volver a ejecutar la prueba.");
    }

    // ---- estructura ----

    @Test
    void v11DebeQuedarAplicadaConSusTriggersYFunciones() throws SQLException {
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT string_agg(version, ',' ORDER BY installed_rank) FROM "
                    + ESQUEMA + ".flyway_schema_history WHERE success")).endsWith(",9,11");
            assertThat(triggersDe(connection, "evaluacion")).isEqualTo("tg_evaluacion_categoria_coherente,"
                    + "tg_evaluacion_clasificacion_inmutable,tg_evaluacion_estado_inicial,"
                    + "tg_evaluacion_estado_transicion,tg_evaluacion_saga_consistente,"
                    + "tg_evaluacion_score_total_consistente");
            assertThat(triggersDe(connection, "trabajo_recomendacion")).isEqualTo("tg_trabajo_recomendacion_inicial,"
                    + "tg_trabajo_recomendacion_saga_consistente,tg_trabajo_recomendacion_sin_borrado,"
                    + "tg_trabajo_recomendacion_sin_truncate,tg_trabajo_recomendacion_transicion");
            assertThat(consultarTexto(connection, "SELECT string_agg(p.proname || ':' || p.prosecdef, ',' "
                    + "ORDER BY p.proname) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                    + "WHERE n.nspname = '" + ESQUEMA + "' AND p.proname IN ('fn_tg_saga_consistente', "
                    + "'reactivar_trabajo_recomendacion', 'fn_tg_trabajo_recomendacion_transicion')"))
                    .isEqualTo("fn_tg_saga_consistente:true,fn_tg_trabajo_recomendacion_transicion:false,"
                            + "reactivar_trabajo_recomendacion:true");
            assertThat(consultarTexto(connection, "SELECT has_function_privilege('public', '" + ESQUEMA
                    + ".reactivar_trabajo_recomendacion(bigint)', 'EXECUTE')::text || ',' || "
                    + "has_function_privilege('public', '" + ESQUEMA + ".fn_tg_saga_consistente()', 'EXECUTE')::text"))
                    .isEqualTo("false,false");
        }
    }

    // ---- I1, I3: creación atómica ----

    @Test
    void unaEvaluacionNuevaDebeInsertarsePendiente() throws SQLException {
        assertRechazo(CHECK_VIOLATION, "debe registrarse en estado PENDIENTE", connection -> ejecutar(connection,
                "INSERT INTO evaluacion (id_solicitante, score_total, categoria, id_politica) VALUES ("
                        + idSolicitante + ", 0, 'BAJO', " + idPolitica + ")"));
        assertRechazo(CHECK_VIOLATION, "debe registrarse en estado PENDIENTE", connection -> ejecutar(connection,
                insertarEvaluacion("COMPLETADA")));
    }

    @Test
    void unaEvaluacionPendienteSinTrabajoSeRechazaAlConfirmarYNoQuedaNada() throws SQLException {
        long[] id = new long[1];
        assertRechazo(CHECK_VIOLATION, "Saga inconsistente", connection ->
                id[0] = insertarDevolviendoId(connection, insertarEvaluacion("PENDIENTE") + " RETURNING id_evaluacion"));
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT count(*)::text FROM evaluacion WHERE id_evaluacion = " + id[0]))
                    .isEqualTo("0");
        }
    }

    @Test
    void laEvaluacionYSuTrabajoSeCreanEnLaMismaTransaccion() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|1|1|" + par.token() + "|-");
    }

    @Test
    void elTrabajoDebeCrearseConLaConcesionDelPrimerIntento() throws SQLException {
        assertRechazo(CHECK_VIOLATION, "debe crearse PENDIENTE", connection -> {
            long id = insertarDevolviendoId(connection, insertarEvaluacion("PENDIENTE") + " RETURNING id_evaluacion");
            ejecutar(connection, "INSERT INTO trabajo_recomendacion (id_evaluacion, intentos, intentos_totales, "
                    + "proximo_intento) VALUES (" + id + ", 0, 0, clock_timestamp())");
        });
    }

    @Test
    void elTriggerADeV8SigueComprobandoElPuntaje() throws SQLException {
        // 5 pertenece a BAJO (trigger B), pero sin detalles la suma es 0 (trigger A, al confirmar)
        assertRechazo(CHECK_VIOLATION, "no coincide con la suma", connection -> {
            long id = insertarDevolviendoId(connection, "INSERT INTO evaluacion (id_solicitante, score_total, "
                    + "categoria, id_politica, estado) VALUES (" + idSolicitante + ", 5, 'BAJO', " + idPolitica
                    + ", 'PENDIENTE') RETURNING id_evaluacion");
            ejecutar(connection, insertarTrabajo(id, UUID.randomUUID(), 30));
        });
    }

    // ---- evaluaciones históricas ----

    @Test
    void unaHistoricaPosteriorAV8AdmiteActualizarOtrasColumnas() throws SQLException {
        enTransaccion(connection -> ejecutar(connection, "UPDATE evaluacion SET fecha_evaluacion = fecha_evaluacion "
                + "WHERE id_evaluacion = " + idHistoricaPosteriorAV8));

        assertThat(estadoDe(idHistoricaPosteriorAV8)).isEqualTo("NULL|-|-|-|-|-");
    }

    @Test
    void unaHistoricaNoPuedeIncorporarseALaSaga() throws SQLException {
        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "Transicion de estado no permitida", connection ->
                ejecutar(connection, "UPDATE evaluacion SET estado = 'PENDIENTE' WHERE id_evaluacion = "
                        + idHistoricaPosteriorAV8));
        assertRechazo(CHECK_VIOLATION, "Saga inconsistente", connection ->
                ejecutar(connection, insertarTrabajo(idHistoricaPosteriorAV8, UUID.randomUUID(), 30)));
    }

    /** Limitación conocida de V8 (V12 aplazada): su CHECK NOT VALID bloquea todo UPDATE de esas filas. */
    @Test
    void unaHistoricaAnteriorAV8SigueBloqueadaPorV8() throws SQLException {
        assertRechazo(CHECK_VIOLATION, "ck_evaluacion_clasificacion_completa", connection ->
                ejecutar(connection, "UPDATE evaluacion SET fecha_evaluacion = fecha_evaluacion "
                        + "WHERE id_evaluacion = " + idHistoricaAnteriorAV8));
    }

    // ---- I2, I3, I4: estado final consistente ----

    @Test
    void completarSinTrabajoProcesadoSeRechazaAlConfirmar() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(CHECK_VIOLATION, "Saga inconsistente", connection -> {
            bloquearEvaluacion(connection, par.id());
            ejecutar(connection, "UPDATE evaluacion SET estado = 'COMPLETADA' WHERE id_evaluacion = " + par.id());
        });
    }

    @Test
    void registrarElExitoCompletaLaEvaluacion() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        registrarExito(par);

        assertThat(estadoDe(par.id())).isEqualTo("COMPLETADA|PROCESADO|1|1|-|-");
    }

    @Test
    void losEstadosIntermediosSeAceptanSiElEstadoFinalEsConsistente() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        enTransaccion(connection -> {
            bloquearEvaluacion(connection, par.id());
            ejecutar(connection, "UPDATE evaluacion SET estado = 'COMPLETADA' WHERE id_evaluacion = " + par.id());
            assertThat(filasActualizadas(connection, procesar(par))).isEqualTo(1);
        });

        assertThat(estadoDe(par.id())).isEqualTo("COMPLETADA|PROCESADO|1|1|-|-");
    }

    @Test
    void conSetConstraintsImmediateLaComprobacionSeAdelantaYRechaza() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(CHECK_VIOLATION, "Saga inconsistente", connection -> {
            ejecutar(connection, "SET CONSTRAINTS ALL IMMEDIATE");
            bloquearEvaluacion(connection, par.id());
            ejecutar(connection, procesar(par));
        });
    }

    @Test
    void unaEvaluacionCompletadaYSuTrabajoProcesadoSonTerminales() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        registrarExito(par);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "Transicion de estado no permitida", connection ->
                ejecutar(connection, "UPDATE evaluacion SET estado = 'PENDIENTE' WHERE id_evaluacion = " + par.id()));
        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "PROCESADO es terminal", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET ultimo_error = 'x' WHERE id_evaluacion = "
                        + par.id()));
    }

    // ---- I6-I12: concesiones y contadores ----

    @Test
    void noSePuedeReclamarConUnaConcesionVigente() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "sigue vigente", connection ->
                ejecutar(connection, reclamar(par.id(), UUID.randomUUID())));
    }

    @Test
    void unaConcesionVencidaSeReclamaDeNuevoYElTokenAntiguoNoModificaNada() throws Exception {
        Par par = nuevaEvaluacionPendiente(1);
        Thread.sleep(1500);
        UUID nuevoToken = UUID.randomUUID();

        enTransaccion(connection -> ejecutar(connection, reclamar(par.id(), nuevoToken)));

        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|2|2|" + nuevoToken + "|-");
        enTransaccion(connection -> {
            bloquearEvaluacion(connection, par.id());
            assertThat(filasActualizadas(connection, procesar(par))).as("token antiguo").isZero();
        });
        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|2|2|" + nuevoToken + "|-");
    }

    @Test
    void unReclamoDebeSumarUnIntentoYVencerEnElFuturo() throws Exception {
        Par par = nuevaEvaluacionPendiente(1);
        Thread.sleep(1500);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "exactamente un intento", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET id_concesion = '" + UUID.randomUUID()
                        + "', concesion_hasta = clock_timestamp() + interval '30 seconds', intentos = intentos + 2, "
                        + "intentos_totales = intentos_totales + 2 WHERE id_evaluacion = " + par.id()));
        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "vencer en el futuro", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET id_concesion = '" + UUID.randomUUID()
                        + "', concesion_hasta = clock_timestamp() - interval '1 second', intentos = intentos + 1, "
                        + "intentos_totales = intentos_totales + 1 WHERE id_evaluacion = " + par.id()));
    }

    @Test
    void unaConcesionNoSePuedeProrrogar() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "no se modifica ni se prorroga", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET concesion_hasta = concesion_hasta "
                        + "+ interval '10 seconds' WHERE id_evaluacion = " + par.id()));
    }

    @Test
    void unErrorTransitorioLiberaLaConcesionSinCambiarLosContadores() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        liberarConErrorTransitorio(par);
        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|1|1|-|SIN_CONEXION");

        Par otro = nuevaEvaluacionPendiente(30);
        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "no cambia los intentos", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET id_concesion = NULL, concesion_hasta = NULL, "
                        + "intentos = 0 WHERE id_evaluacion = " + otro.id()));
    }

    @Test
    void unExitoSoloSeRegistraLiberandoUnaConcesion() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        liberarConErrorTransitorio(par);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "liberando una concesion", connection -> {
            bloquearEvaluacion(connection, par.id());
            ejecutar(connection, "UPDATE trabajo_recomendacion SET estado_trabajo = 'PROCESADO', "
                    + "procesado_en = clock_timestamp() WHERE id_evaluacion = " + par.id());
        });
    }

    @Test
    void intentosTotalesSoloAumentaAlReclamar() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "solo aumenta al reclamar", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET intentos_totales = intentos_totales + 1 "
                        + "WHERE id_evaluacion = " + par.id()));
    }

    @Test
    void unaConcesionVencidaPuedeAgotarseSinCambiarLosContadores() throws Exception {
        Par par = nuevaEvaluacionPendiente(1);
        Thread.sleep(1500);

        enTransaccion(connection -> {
            bloquearEvaluacion(connection, par.id());
            assertThat(filasActualizadas(connection, "UPDATE trabajo_recomendacion SET estado_trabajo = "
                    + "'ERROR_PERMANENTE', id_concesion = NULL, concesion_hasta = NULL, codigo_ultimo_error = "
                    + "'CONCESIONES_AGOTADAS', actualizado_en = clock_timestamp() WHERE id_evaluacion = " + par.id()
                    + " AND estado_trabajo = 'PENDIENTE' AND intentos >= 1 AND (concesion_hasta IS NULL "
                    + "OR concesion_hasta < clock_timestamp())")).isEqualTo(1);
        });

        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|ERROR_PERMANENTE|1|1|-|CONCESIONES_AGOTADAS");
    }

    // ---- I5: reactivación ----

    @Test
    void laReactivacionPorFuncionPoneIntentosACeroYConservaElHistorial() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        marcarErrorPermanente(par);

        enTransaccion(connection -> ejecutar(connection, "SELECT reactivar_trabajo_recomendacion(" + par.id() + ")"));
        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|0|1|-|RECHAZO_FUNCIONAL");

        UUID nuevoToken = UUID.randomUUID();
        enTransaccion(connection -> ejecutar(connection, reclamar(par.id(), nuevoToken)));
        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|1|2|" + nuevoToken + "|RECHAZO_FUNCIONAL");
    }

    @Test
    void laReactivacionExigeUnTrabajoEnErrorPermanente() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "no esta en ERROR_PERMANENTE", connection ->
                ejecutar(connection, "SELECT reactivar_trabajo_recomendacion(" + par.id() + ")"));
    }

    @Test
    void unaReactivacionDirectaDebePonerLosIntentosACero() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        marcarErrorPermanente(par);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "pone intentos a cero", connection ->
                ejecutar(connection, "UPDATE trabajo_recomendacion SET estado_trabajo = 'PENDIENTE' "
                        + "WHERE id_evaluacion = " + par.id()));
    }

    /**
     * Limitación conocida (decisión 4): el propietario de la tabla puede reactivar sin la función. Aquí
     * el rol de la prueba es el propietario; con roles separados, la aplicación no podría hacerlo.
     */
    @Test
    void limitacionConocidaElPropietarioPuedeReactivarDirectamente() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        marcarErrorPermanente(par);

        enTransaccion(connection -> ejecutar(connection, "UPDATE trabajo_recomendacion SET estado_trabajo = "
                + "'PENDIENTE', intentos = 0 WHERE id_evaluacion = " + par.id()));

        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|0|1|-|RECHAZO_FUNCIONAL");
    }

    // ---- I13 ----

    @Test
    void losTrabajosNoSePuedenBorrarNiTruncar() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "no pueden eliminarse", connection ->
                ejecutar(connection, "DELETE FROM trabajo_recomendacion WHERE id_evaluacion = " + par.id()));
        assertRechazo(OBJECT_NOT_IN_PREREQUISITE_STATE, "no pueden eliminarse", connection ->
                ejecutar(connection, "TRUNCATE trabajo_recomendacion"));
    }

    // ---- concurrencia (dos conexiones) ----

    @Test
    void dosReclamosSimultaneosNoTomanElMismoTrabajo() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);
        liberarConErrorTransitorio(par);
        String seleccion = "SELECT id_evaluacion FROM trabajo_recomendacion WHERE id_evaluacion = " + par.id()
                + " AND estado_trabajo = 'PENDIENTE' AND proximo_intento <= clock_timestamp() "
                + "AND (concesion_hasta IS NULL OR concesion_hasta < clock_timestamp()) "
                + "FOR NO KEY UPDATE SKIP LOCKED";

        try (Connection primera = conectar(); Connection segunda = conectar()) {
            primera.setAutoCommit(false);
            segunda.setAutoCommit(false);
            try {
                assertThat(contarFilas(primera, seleccion)).isEqualTo(1);
                assertThat(contarFilas(segunda, seleccion)).isZero();
            } finally {
                primera.rollback();
                segunda.rollback();
            }
        }
    }

    /**
     * Cambio concurrente que se salta el orden canónico a propósito: la primera conexión procesa el
     * trabajo sin bloquear la evaluación y la segunda completa la evaluación. La comprobación diferida
     * de la segunda espera al trabajo bloqueado y vence su lock_timeout; la de la primera ve después la
     * evaluación PENDIENTE y rechaza. Resultado: espera o error, nunca un estado inconsistente.
     */
    @Test
    void unCambioConcurrenteDelParNoDejaUnEstadoInconsistente() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        try (Connection primera = conectar(); Connection segunda = conectar()) {
            primera.setAutoCommit(false);
            segunda.setAutoCommit(false);
            try {
                assertThat(filasActualizadas(primera, procesar(par))).isEqualTo(1);

                ejecutar(segunda, "SET LOCAL lock_timeout = '1s'");
                ejecutar(segunda, "UPDATE evaluacion SET estado = 'COMPLETADA' WHERE id_evaluacion = " + par.id());
                assertThatThrownBy(segunda::commit).isInstanceOfSatisfying(SQLException.class,
                        ex -> assertThat(ex.getSQLState()).isEqualTo(LOCK_NOT_AVAILABLE));

                assertThatThrownBy(primera::commit).isInstanceOfSatisfying(SQLException.class, ex -> {
                    assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION);
                    assertThat(ex.getMessage()).contains("Saga inconsistente");
                });
            } finally {
                primera.rollback();
                segunda.rollback();
            }
        }

        assertThat(estadoDe(par.id())).isEqualTo("PENDIENTE|PENDIENTE|1|1|" + par.token() + "|-");
    }

    @Test
    void elOrdenCanonicoSerializaLasOperacionesSobreUnaEvaluacion() throws SQLException {
        Par par = nuevaEvaluacionPendiente(30);

        try (Connection registro = conectar(); Connection agotamiento = conectar()) {
            registro.setAutoCommit(false);
            agotamiento.setAutoCommit(false);
            try {
                bloquearEvaluacion(registro, par.id());

                ejecutar(agotamiento, "SET LOCAL lock_timeout = '1s'");
                assertThatThrownBy(() -> bloquearEvaluacion(agotamiento, par.id()))
                        .isInstanceOfSatisfying(SQLException.class,
                                ex -> assertThat(ex.getSQLState()).isEqualTo(LOCK_NOT_AVAILABLE));
            } finally {
                registro.rollback();
                agotamiento.rollback();
            }
        }
    }

    // ---- operaciones de la aplicación (orden canónico: evaluación y después trabajo) ----

    private static Par nuevaEvaluacionPendiente(int segundosDeConcesion) throws SQLException {
        UUID token = UUID.randomUUID();
        long[] id = new long[1];
        enTransaccion(connection -> {
            id[0] = insertarDevolviendoId(connection, insertarEvaluacion("PENDIENTE") + " RETURNING id_evaluacion");
            ejecutar(connection, insertarTrabajo(id[0], token, segundosDeConcesion));
        });
        return new Par(id[0], token);
    }

    private static void registrarExito(Par par) throws SQLException {
        enTransaccion(connection -> {
            bloquearEvaluacion(connection, par.id());
            assertThat(filasActualizadas(connection, procesar(par))).isEqualTo(1);
            assertThat(filasActualizadas(connection, "UPDATE evaluacion SET estado = 'COMPLETADA' "
                    + "WHERE id_evaluacion = " + par.id() + " AND estado = 'PENDIENTE'")).isEqualTo(1);
        });
    }

    private static void liberarConErrorTransitorio(Par par) throws SQLException {
        enTransaccion(connection -> {
            bloquearEvaluacion(connection, par.id());
            assertThat(filasActualizadas(connection, "UPDATE trabajo_recomendacion SET id_concesion = NULL, "
                    + "concesion_hasta = NULL, proximo_intento = clock_timestamp(), codigo_ultimo_error = "
                    + "'SIN_CONEXION', actualizado_en = clock_timestamp() WHERE id_evaluacion = " + par.id()
                    + " AND id_concesion = '" + par.token() + "'")).isEqualTo(1);
        });
    }

    private static void marcarErrorPermanente(Par par) throws SQLException {
        enTransaccion(connection -> {
            bloquearEvaluacion(connection, par.id());
            assertThat(filasActualizadas(connection, "UPDATE trabajo_recomendacion SET estado_trabajo = "
                    + "'ERROR_PERMANENTE', id_concesion = NULL, concesion_hasta = NULL, codigo_ultimo_error = "
                    + "'RECHAZO_FUNCIONAL', actualizado_en = clock_timestamp() WHERE id_evaluacion = " + par.id()
                    + " AND id_concesion = '" + par.token() + "'")).isEqualTo(1);
        });
    }

    private static String procesar(Par par) {
        return "UPDATE trabajo_recomendacion SET estado_trabajo = 'PROCESADO', procesado_en = clock_timestamp(), "
                + "id_concesion = NULL, concesion_hasta = NULL, actualizado_en = clock_timestamp() "
                + "WHERE id_evaluacion = " + par.id() + " AND id_concesion = '" + par.token() + "'";
    }

    private static String reclamar(long idEvaluacion, UUID token) {
        return "UPDATE trabajo_recomendacion SET id_concesion = '" + token + "', concesion_hasta = clock_timestamp() "
                + "+ interval '30 seconds', intentos = intentos + 1, intentos_totales = intentos_totales + 1, "
                + "actualizado_en = clock_timestamp() WHERE id_evaluacion = " + idEvaluacion;
    }

    private static String insertarEvaluacion(String estado) {
        // puntaje 0 sin detalles (trigger A) y categoría BAJO (trigger B)
        return "INSERT INTO evaluacion (id_solicitante, score_total, categoria, id_politica, estado) VALUES ("
                + idSolicitante + ", 0, 'BAJO', " + idPolitica + ", '" + estado + "')";
    }

    private static String insertarTrabajo(long idEvaluacion, UUID token, int segundosDeConcesion) {
        return "INSERT INTO trabajo_recomendacion (id_evaluacion, intentos, intentos_totales, proximo_intento, "
                + "id_concesion, concesion_hasta) VALUES (" + idEvaluacion + ", 1, 1, clock_timestamp(), '" + token
                + "', clock_timestamp() + make_interval(secs => " + segundosDeConcesion + "))";
    }

    private static void bloquearEvaluacion(Connection connection, long idEvaluacion) throws SQLException {
        contarFilas(connection, "SELECT 1 FROM evaluacion WHERE id_evaluacion = " + idEvaluacion + " FOR NO KEY UPDATE");
    }

    /** estado|estado_trabajo|intentos|intentos_totales|id_concesion|codigo_ultimo_error ("-" o "NULL" si faltan). */
    private static String estadoDe(long idEvaluacion) throws SQLException {
        try (Connection connection = conectar()) {
            return consultarTexto(connection, "SELECT concat_ws('|', coalesce(e.estado, 'NULL'), "
                    + "coalesce(t.estado_trabajo, '-'), coalesce(t.intentos::text, '-'), "
                    + "coalesce(t.intentos_totales::text, '-'), coalesce(t.id_concesion::text, '-'), "
                    + "coalesce(t.codigo_ultimo_error, '-')) FROM evaluacion e "
                    + "LEFT JOIN trabajo_recomendacion t ON t.id_evaluacion = e.id_evaluacion "
                    + "WHERE e.id_evaluacion = " + idEvaluacion);
        }
    }

    // ---- utilidades ----

    /** Ejecuta la acción y confirma; exige que falle (en la sentencia o al confirmar) con ese SQLSTATE. */
    private static void assertRechazo(String sqlState, String fragmento, Accion accion) throws SQLException {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                assertThatThrownBy(() -> {
                    accion.ejecutar(connection);
                    connection.commit();
                }).isInstanceOfSatisfying(SQLException.class, ex -> {
                    assertThat(ex.getSQLState()).isEqualTo(sqlState);
                    assertThat(ex.getMessage()).contains(fragmento);
                });
            } finally {
                connection.rollback();
            }
        }
    }

    private static void enTransaccion(Accion accion) throws SQLException {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                accion.ejecutar(connection);
                connection.commit();
            } catch (SQLException | RuntimeException | Error ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    /** Política con rangos BAJO [-100, 29], MEDIO [30, 79] y ALTO [80, 130] (ejemplo del Gherkin de HU11). */
    private static long publicarPolitica(Connection connection) throws SQLException {
        long id = insertarDevolviendoId(connection, "INSERT INTO politica_clasificacion (version, publicado_por) "
                + "VALUES (1, '" + UUID.randomUUID() + "') RETURNING id_politica");
        ejecutar(connection, "INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, limite_superior) "
                + "VALUES (" + id + ", 'BAJO', -100, 29), (" + id + ", 'MEDIO', 30, 79), (" + id + ", 'ALTO', 80, 130)");
        return id;
    }

    private static String triggersDe(Connection connection, String tabla) throws SQLException {
        return consultarTexto(connection, "SELECT string_agg(t.tgname, ',' ORDER BY t.tgname) FROM pg_trigger t "
                + "JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = '" + ESQUEMA + "' AND NOT t.tgisinternal AND c.relname = '" + tabla + "'");
    }

    private static String motivoParaNoEliminarElEsquema(Connection connection) throws SQLException {
        if (oidEsquemaCreado == null) {
            return "no se registró el OID del esquema tras su creación";
        }
        ejecutar(connection, "LOCK TABLE " + ESQUEMA + ".flyway_schema_history IN ACCESS EXCLUSIVE MODE");
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT n.oid, (SELECT count(*) FROM " + ESQUEMA + ".flyway_schema_history WHERE type = 'SCHEMA') "
                        + "FROM pg_namespace n WHERE n.nspname = ?")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return "el esquema ya no existe";
                }
                if (resultSet.getLong(1) != oidEsquemaCreado) {
                    return "el esquema existente no es el que creó esta ejecución (OID distinto)";
                }
                if (resultSet.getLong(2) != 1) {
                    return "el historial de Flyway no contiene exactamente una marca de creación de esquema";
                }
            }
        }
        return null;
    }

    private static Long registrarEsquemaCreadoPorFlyway() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, usuario, clave);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT n.oid, (SELECT count(*) FROM " + ESQUEMA + ".flyway_schema_history WHERE type = 'SCHEMA') "
                                + "FROM pg_namespace n WHERE n.nspname = ?")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("el esquema " + ESQUEMA + " debe existir").isTrue();
                assertThat(resultSet.getLong(2)).as("marcas de creación de esquema de Flyway").isEqualTo(1);
                return resultSet.getLong(1);
            }
        }
    }

    private static Flyway flyway(String version) {
        return Flyway.configure()
                .dataSource(url, usuario, clave)
                .schemas(ESQUEMA)
                .locations(UBICACION_MIGRACIONES, UBICACION_PROPUESTA)
                .cleanDisabled(true)
                .target(version)
                .load();
    }

    /** Conexión cuyo search_path es únicamente el esquema aislado, una vez creado. */
    private static Connection conectar() throws SQLException {
        Connection connection = DriverManager.getConnection(url, usuario, clave);
        if (esquemaCreadoPorLaPrueba) {
            ejecutar(connection, "SET search_path TO " + ESQUEMA);
        }
        return connection;
    }

    private static long insertarDevolviendoId(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static int contarFilas(Connection connection, String sql) throws SQLException {
        int filas = 0;
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                filas++;
            }
        }
        return filas;
    }

    private static int filasActualizadas(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private static void ejecutar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String consultarTexto(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            String valor = resultSet.getString(1);
            return valor == null ? "" : valor;
        }
    }
}
