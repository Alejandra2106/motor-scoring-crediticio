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
 * HU12: aplicación de la migración V8 (src/main/resources/db/migration, validada con esta prueba
 * antes de su promoción) y comportamiento de sus restricciones y triggers A, B y C, en un esquema
 * aislado y temporal.
 *
 * Antes de abrir cualquier conexión exige, por igualdad exacta de texto, que -Dit.base-dedicada sea
 * motor_scoring_it y que -Dspring.datasource.url sea jdbc:postgresql://localhost:5432/motor_scoring_it
 * (cualquier otro host, puerto, base o parámetro se rechaza; nunca usa DB_URL). Después comprueba
 * con current_database() que la conexión llega a esa base y exige que el esquema no exista.
 * Aplica V1-V7, inserta una evaluación histórica (sin clasificación), aplica V8 y verifica. No toca
 * el esquema public ni sus datos.
 *
 * Limpieza: solo elimina hu12_v8_it si la base demuestra que lo creó esta ejecución: el esquema
 * conserva el OID registrado justo después de que Flyway lo creara y su historial contiene la marca
 * de creación de esquema de Flyway (type = 'SCHEMA'). Si cualquiera de esas comprobaciones falla, es
 * ambigua o no puede completarse, no elimina nada y hace fallar la ejecución indicando el motivo.
 *
 * Al terminar en "IT", Surefire no la ejecuta con `./mvnw test`. Ejecución manual (requiere
 * autorización expresa para usar motor_scoring_it), desde la raíz del proyecto, sin
 * -Dsurefire.failIfNoSpecifiedTests=false (un nombre de prueba incorrecto debe hacer fallar Maven):
 *   mvnw.cmd -B test "-Dtest=EvaluacionV8MigracionIT"
 *     "-Dit.base-dedicada=motor_scoring_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
 * con DB_USERNAME y DB_PASSWORD en el entorno. Resultado esperado: "Tests run: 13".
 */
class EvaluacionV8MigracionIT {

    private static final String ESQUEMA = "hu12_v8_it";
    /** Únicos valores admitidos; se comparan por igualdad exacta antes de abrir cualquier conexión. */
    private static final String BASE_AUTORIZADA = "motor_scoring_it";
    private static final String URL_AUTORIZADA = "jdbc:postgresql://localhost:5432/" + BASE_AUTORIZADA;
    private static final String UBICACION_MIGRACIONES = "classpath:db/migration";
    private static final String UBICACION_PROPUESTA = "filesystem:database/propuestas/hu12";

    private static final String CHECK_VIOLATION = "23514";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String OBJECT_NOT_IN_PREREQUISITE_STATE = "55000";

    private static String url;
    private static String usuario;
    private static String clave;
    /** Solo habilita el search_path del esquema; la limpieza no se basa en esta marca. */
    private static boolean esquemaCreadoPorLaPrueba;
    /** OID del esquema leído justo después de que Flyway lo creara; null si no llegó a registrarse. */
    private static Long oidEsquemaCreado;

    private static long idSolicitante;
    private static long idRegla;
    private static long idPolitica;
    private static long idEvaluacionHistorica;

    @BeforeAll
    static void prepararEsquemaConEvaluacionHistoricaYAplicarV8() throws SQLException {
        String base = System.getProperty("it.base-dedicada");
        url = System.getProperty("spring.datasource.url");
        usuario = System.getenv("DB_USERNAME");
        clave = System.getenv("DB_PASSWORD");
        verificarConfiguracionSinConectarse(base, url);
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
                            + "VALUES ('Solicitante HU12', '900000001', 4000000, 1200000, 0, 'BUENO', 2) "
                            + "RETURNING id_solicitante");
            long idRiesgo = insertarDevolviendoId(connection,
                    "INSERT INTO riesgo (variable, descripcion) VALUES ('INGRESOS_MENSUALES', "
                            + "'Ingresos mensuales del solicitante') RETURNING id_riesgo");
            idRegla = insertarDevolviendoId(connection,
                    "INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) "
                            + "VALUES (" + idRiesgo + ", '>=', '3000000', 20) RETURNING id_regla");
            idPolitica = insertarDevolviendoId(connection,
                    "INSERT INTO politica_clasificacion (version, publicado_por) VALUES (1, '"
                            + UUID.randomUUID() + "') RETURNING id_politica");
            ejecutar(connection, "INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, "
                    + "limite_superior) VALUES (" + idPolitica + ", 'ALTO', -100, 39), (" + idPolitica
                    + ", 'MEDIO', 40, 79), (" + idPolitica + ", 'BAJO', 80, 130)");
            // evaluación anterior a V8: sin puntaje persistido ni clasificación
            idEvaluacionHistorica = insertarDevolviendoId(connection,
                    "INSERT INTO evaluacion (id_solicitante) VALUES (" + idSolicitante + ") RETURNING id_evaluacion");
            insertarDetalle(connection, idEvaluacionHistorica, 20);
        }

        // explícitamente hasta V8: esta es la prueba de V8; migraciones posteriores (V9 hace inmutables
        // las políticas) romperían su caso de borrado de una política referenciada
        flyway("8").migrate();
    }

    /**
     * Elimina hu12_v8_it solo con evidencia de la base de que lo creó esta ejecución (ver
     * {@link #motivoParaNoEliminarElEsquema}). En cualquier otro caso no elimina nada y falla
     * informando el motivo, para que el esquema se revise manualmente.
     */
    @AfterAll
    static void eliminarSoloElEsquemaCreadoPorLaPrueba() throws SQLException {
        if (!esquemaCreadoPorLaPrueba) {
            return; // la prueba abortó antes de crear nada
        }
        String motivo;
        FaseDeLimpieza fase = FaseDeLimpieza.CONEXION;
        try (Connection connection = DriverManager.getConnection(url, usuario, clave)) {
            try {
                connection.setAutoCommit(false);
                fase = FaseDeLimpieza.COMPROBACION;
                motivo = motivoParaNoEliminarElEsquema(connection);
                if (motivo == null) {
                    fase = FaseDeLimpieza.DROP;
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("DROP SCHEMA " + ESQUEMA + " CASCADE");
                    }
                    fase = FaseDeLimpieza.COMMIT;
                    connection.commit();
                    fase = FaseDeLimpieza.CONFIRMADA;
                    return;
                }
                connection.rollback();
            } catch (SQLException ex) {
                revertirSinOcultarElError(connection, ex);
                throw ex;
            }
        } catch (SQLException ex) {
            if (fase == FaseDeLimpieza.CONFIRMADA) {
                return; // la eliminación ya se confirmó; solo falló el cierre de la conexión
            }
            motivo = fase.describir(ex.getSQLState());
        }
        throw new AssertionError("No se eliminó el esquema " + ESQUEMA + ": " + motivo
                + ". Revíselo manualmente antes de volver a ejecutar la prueba.");
    }

    /**
     * Revierte la transacción de limpieza tras un error. Si la reversión también falla, el error se
     * adjunta al original; al cerrarse la conexión, PostgreSQL aborta igualmente la transacción
     * abierta, de modo que nunca se confirma una eliminación a medias.
     */
    private static void revertirSinOcultarElError(Connection connection, SQLException error) {
        try {
            connection.rollback();
        } catch (SQLException errorAlRevertir) {
            error.addSuppressed(errorAlRevertir);
        }
    }

    /** Fase de la limpieza en la que ocurrió un error, para informar el motivo sin ambigüedad. */
    private enum FaseDeLimpieza {
        CONEXION("no fue posible abrir la conexión de limpieza; no se ejecutó ninguna comprobación ni eliminación"),
        COMPROBACION("falló la comprobación de si es seguro eliminar el esquema; la transacción se revirtió sin "
                + "eliminar nada"),
        DROP("las comprobaciones se superaron, pero falló DROP SCHEMA; la transacción se revirtió y el esquema no "
                + "se eliminó"),
        COMMIT("falló la confirmación de la transacción que contenía DROP SCHEMA; PostgreSQL no aplica una "
                + "transacción cuyo COMMIT falla, pero si el fallo fue de comunicación el resultado es incierto: "
                + "compruebe si el esquema sigue existiendo"),
        CONFIRMADA("la eliminación se confirmó");

        private final String descripcion;

        FaseDeLimpieza(String descripcion) {
            this.descripcion = descripcion;
        }

        String describir(String sqlState) {
            return descripcion + " (SQLState " + sqlState + ")";
        }
    }

    /**
     * Devuelve null solo si todas las comprobaciones se cumplen sin ambigüedad, dentro de la misma
     * transacción que hará el DROP. Primero bloquea (ACCESS EXCLUSIVE) la tabla de historial de
     * Flyway del esquema: mientras dure la transacción, ninguna otra sesión puede eliminar ni
     * recrear el esquema entre la comprobación y el DROP. Después exige:
     * 1. Que se haya registrado el OID del esquema justo después de que Flyway lo creara.
     * 2. Que exista exactamente un esquema hu12_v8_it y que su OID sea ese mismo (no recreado por otro).
     * 3. Que su historial de Flyway contenga exactamente una marca de creación de esquema
     *    (type = 'SCHEMA'): Flyway solo la escribe cuando crea él mismo el esquema.
     * Cualquier error (por ejemplo, que la tabla de historial no exista) se propaga y la limpieza
     * se abstiene de eliminar.
     */
    private static String motivoParaNoEliminarElEsquema(Connection connection) throws SQLException {
        if (oidEsquemaCreado == null) {
            return "no se registró el OID del esquema tras su creación";
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + ESQUEMA + ".flyway_schema_history IN ACCESS EXCLUSIVE MODE");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT oid FROM pg_namespace WHERE nspname = ?")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return "el esquema ya no existe";
                }
                long oidActual = resultSet.getLong(1);
                if (resultSet.next()) {
                    return "la consulta del esquema devolvió más de una fila";
                }
                if (oidActual != oidEsquemaCreado) {
                    return "el esquema existente no es el que creó esta ejecución (OID distinto)";
                }
            }
        }
        long marcas = contarMarcasDeCreacionDeFlyway(connection);
        if (marcas != 1) {
            return "el historial de Flyway contiene " + marcas + " marcas de creación de esquema (se esperaba 1)";
        }
        return null;
    }

    /**
     * Justo después de la primera migración: exige la marca de creación de esquema de Flyway y
     * registra el OID del esquema. Si la marca no existe, el esquema no lo creó esta ejecución.
     */
    private static Long registrarEsquemaCreadoPorFlyway() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, usuario, clave)) {
            long marcas = contarMarcasDeCreacionDeFlyway(connection);
            assertThat(marcas).as("marcas de creación de esquema de Flyway en " + ESQUEMA).isEqualTo(1);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT oid FROM pg_namespace WHERE nspname = ?")) {
                statement.setString(1, ESQUEMA);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).as("el esquema " + ESQUEMA + " debe existir").isTrue();
                    return resultSet.getLong(1);
                }
            }
        }
    }

    private static long contarMarcasDeCreacionDeFlyway(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM " + ESQUEMA + ".flyway_schema_history WHERE type = 'SCHEMA'")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    @Test
    void v8DebeQuedarAplicadaConSusColumnasRestriccionesYTriggers() throws SQLException {
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT string_agg(version, ',' ORDER BY installed_rank) FROM "
                    + ESQUEMA + ".flyway_schema_history WHERE success")).endsWith(",7,8");
            assertThat(consultarTexto(connection, "SELECT string_agg(column_name || ':' || data_type, ',' ORDER BY "
                    + "column_name) FROM information_schema.columns WHERE table_schema = '" + ESQUEMA
                    + "' AND table_name = 'evaluacion' AND column_name IN ('score_total', 'categoria', 'id_politica')"))
                    .isEqualTo("categoria:character varying,id_politica:bigint,score_total:integer");
            assertThat(consultarTexto(connection, "SELECT string_agg(conname || ':' || convalidated, ',' ORDER BY "
                    + "conname) FROM pg_constraint WHERE conrelid = '" + ESQUEMA + ".evaluacion'::regclass "
                    + "AND conname IN ('fk_evaluacion_politica', 'ck_evaluacion_categoria', "
                    + "'ck_evaluacion_clasificacion_completa')"))
                    .isEqualTo("ck_evaluacion_categoria:true,ck_evaluacion_clasificacion_completa:false,"
                            + "fk_evaluacion_politica:true");
            assertThat(consultarTexto(connection, "SELECT string_agg(tgname, ',' ORDER BY tgname) FROM pg_trigger t "
                    + "JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_namespace n ON n.oid = c.relnamespace "
                    + "WHERE n.nspname = '" + ESQUEMA + "' AND NOT t.tgisinternal"))
                    .isEqualTo("tg_detalle_evaluacion_score_total_consistente,tg_evaluacion_categoria_coherente,"
                            + "tg_evaluacion_clasificacion_inmutable,tg_evaluacion_score_total_consistente");
        }
    }

    @Test
    void laEvaluacionHistoricaDebeConservarseSinClasificacion() throws SQLException {
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT concat_ws('|', score_total, categoria, id_politica) FROM "
                    + "evaluacion WHERE id_evaluacion = " + idEvaluacionHistorica)).isEmpty();
            assertThat(consultarTexto(connection, "SELECT SUM(puntaje_obtenido)::text FROM detalle_evaluacion "
                    + "WHERE id_evaluacion = " + idEvaluacionHistorica)).isEqualTo("20");
        }
    }

    @Test
    void triggerCDebeImpedirClasificarRetroactivamenteUnaEvaluacionHistorica() throws SQLException {
        enTransaccionRevertida(connection -> assertThatThrownBy(() -> ejecutar(connection,
                "UPDATE evaluacion SET score_total = 20, categoria = 'ALTO', id_politica = " + idPolitica
                        + " WHERE id_evaluacion = " + idEvaluacionHistorica))
                .isInstanceOfSatisfying(SQLException.class,
                        ex -> assertThat(ex.getSQLState()).isEqualTo(OBJECT_NOT_IN_PREREQUISITE_STATE)));
    }

    @Test
    void unaEvaluacionNuevaSinClasificacionDebeRechazarse() throws SQLException {
        enTransaccionRevertida(connection -> assertThatThrownBy(() -> ejecutar(connection,
                "INSERT INTO evaluacion (id_solicitante) VALUES (" + idSolicitante + ")"))
                .isInstanceOfSatisfying(SQLException.class, ex -> {
                    assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION);
                    assertThat(ex.getMessage()).contains("ck_evaluacion_clasificacion_completa");
                }));
    }

    @Test
    void unaEvaluacionClasificadaYConsistenteDebeConfirmarse() throws SQLException {
        long idEvaluacion;
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            idEvaluacion = insertarEvaluacionClasificada(connection, 20, "ALTO");
            insertarDetalle(connection, idEvaluacion, 20);
            connection.commit();
        }
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT concat_ws('|', score_total, categoria, id_politica) FROM "
                    + "evaluacion WHERE id_evaluacion = " + idEvaluacion)).isEqualTo("20|ALTO|" + idPolitica);
        }
    }

    @Test
    void triggerADebeRechazarAlConfirmarUnPuntajeDistintoDeLaSumaDeLosDetalles() throws SQLException {
        long antes = contarEvaluaciones();
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            long idEvaluacion = insertarEvaluacionClasificada(connection, 30, "ALTO");
            insertarDetalle(connection, idEvaluacion, 20);
            assertThatThrownBy(connection::commit).isInstanceOfSatisfying(SQLException.class, ex -> {
                assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION);
                assertThat(ex.getMessage()).contains("no coincide con la suma de sus detalles");
            });
        }
        assertThat(contarEvaluaciones()).as("nada debe persistir (CA08)").isEqualTo(antes);
    }

    @Test
    void triggerADebeRechazarUnaEvaluacionConPuntajeSinLosDetallesQueLoSustentan() throws SQLException {
        long antes = contarEvaluaciones();
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            insertarEvaluacionClasificada(connection, 20, "ALTO");
            assertThatThrownBy(connection::commit).isInstanceOfSatisfying(SQLException.class,
                    ex -> assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION));
        }
        assertThat(contarEvaluaciones()).isEqualTo(antes);
    }

    @Test
    void triggerADebeImpedirEliminarUnDetalleDeUnaEvaluacionClasificada() throws SQLException {
        long idEvaluacion;
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            idEvaluacion = insertarEvaluacionClasificada(connection, 20, "ALTO");
            insertarDetalle(connection, idEvaluacion, 20);
            connection.commit();
        }
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            ejecutar(connection, "DELETE FROM detalle_evaluacion WHERE id_evaluacion = " + idEvaluacion);
            assertThatThrownBy(connection::commit).isInstanceOfSatisfying(SQLException.class,
                    ex -> assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION));
        }
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT count(*)::text FROM detalle_evaluacion WHERE id_evaluacion = "
                    + idEvaluacion)).isEqualTo("1");
        }
    }

    @Test
    void triggerBDebeRechazarUnaCategoriaQueNoCorrespondeAlPuntaje() throws SQLException {
        enTransaccionRevertida(connection -> assertThatThrownBy(() -> insertarEvaluacionClasificada(connection, 20,
                "BAJO"))
                .isInstanceOfSatisfying(SQLException.class, ex -> {
                    assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION);
                    assertThat(ex.getMessage()).contains("no corresponde al puntaje");
                }));
    }

    @Test
    void triggerBDebeRechazarUnPuntajeFueraDeTodosLosRangosDeLaPolitica() throws SQLException {
        enTransaccionRevertida(connection -> assertThatThrownBy(() -> insertarEvaluacionClasificada(connection, 131,
                "BAJO"))
                .isInstanceOfSatisfying(SQLException.class,
                        ex -> assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION)));
    }

    /**
     * Con una política inexistente el trigger B (BEFORE INSERT) no encuentra rangos y rechaza la
     * fila antes de que se compruebe la clave foránea; la clave foránea queda como segunda defensa.
     */
    @Test
    void laEvaluacionDebeReferenciarUnaPoliticaExistente() throws SQLException {
        enTransaccionRevertida(connection -> assertThatThrownBy(() -> ejecutar(connection,
                "INSERT INTO evaluacion (id_solicitante, score_total, categoria, id_politica) VALUES ("
                        + idSolicitante + ", 20, 'ALTO', -1)"))
                .isInstanceOfSatisfying(SQLException.class, ex -> {
                    assertThat(ex.getSQLState()).isEqualTo(CHECK_VIOLATION);
                    assertThat(ex.getMessage()).contains("no corresponde al puntaje");
                }));
    }

    @Test
    void triggerCDebeImpedirModificarLaClasificacionPeroNoOtrasColumnas() throws SQLException {
        long idEvaluacion;
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            idEvaluacion = insertarEvaluacionClasificada(connection, 20, "ALTO");
            insertarDetalle(connection, idEvaluacion, 20);
            connection.commit();
        }
        enTransaccionRevertida(connection -> {
            assertThatThrownBy(() -> ejecutar(connection,
                    "UPDATE evaluacion SET categoria = 'MEDIO' WHERE id_evaluacion = " + idEvaluacion))
                    .isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getSQLState()).isEqualTo(OBJECT_NOT_IN_PREREQUISITE_STATE));
        });
        enTransaccionRevertida(connection -> ejecutar(connection,
                "UPDATE evaluacion SET fecha_evaluacion = fecha_evaluacion WHERE id_evaluacion = " + idEvaluacion));
    }

    @Test
    void unaPoliticaReferenciadaPorUnaEvaluacionNoDebePoderEliminarse() throws SQLException {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            long idEvaluacion = insertarEvaluacionClasificada(connection, 20, "ALTO");
            insertarDetalle(connection, idEvaluacion, 20);
            connection.commit();
        }
        enTransaccionRevertida(connection -> {
            ejecutar(connection, "DELETE FROM rango_clasificacion WHERE id_politica = " + idPolitica);
            assertThatThrownBy(() -> ejecutar(connection,
                    "DELETE FROM politica_clasificacion WHERE id_politica = " + idPolitica))
                    .isInstanceOfSatisfying(SQLException.class, ex -> {
                        assertThat(ex.getSQLState()).isEqualTo(FOREIGN_KEY_VIOLATION);
                        assertThat(ex.getMessage()).contains("fk_evaluacion_politica");
                    });
        });
    }

    // ---------------------------------------------------------------------------------------

    /**
     * Validación sin abrir ninguna conexión, por igualdad exacta: solo se admiten la base
     * motor_scoring_it y la URL jdbc:postgresql://localhost:5432/motor_scoring_it. Cualquier otro
     * host, puerto, base, parámetro o variación de mayúsculas se rechaza.
     */
    private static void verificarConfiguracionSinConectarse(String base, String urlConfigurada) {
        if (!BASE_AUTORIZADA.equals(base)) {
            throw new IllegalStateException("Debe indicarse -Dit.base-dedicada=" + BASE_AUTORIZADA);
        }
        if (!URL_AUTORIZADA.equals(urlConfigurada)) {
            throw new IllegalStateException("spring.datasource.url debe indicarse con -D y ser exactamente "
                    + URL_AUTORIZADA);
        }
    }

    private static Flyway flyway(String version) {
        var configuracion = Flyway.configure()
                .dataSource(url, usuario, clave)
                .schemas(ESQUEMA)
                .locations(UBICACION_MIGRACIONES, UBICACION_PROPUESTA)
                .cleanDisabled(true);
        if (version != null) {
            configuracion = configuracion.target(version);
        }
        return configuracion.load();
    }

    /** Conexión cuyo search_path es únicamente el esquema aislado. */
    private static Connection conectar() throws SQLException {
        Connection connection = DriverManager.getConnection(url, usuario, clave);
        if (esquemaCreadoPorLaPrueba) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + ESQUEMA);
            }
        }
        return connection;
    }

    private static long insertarEvaluacionClasificada(Connection connection, int puntaje, String categoria)
            throws SQLException {
        return insertarDevolviendoId(connection, "INSERT INTO evaluacion (id_solicitante, score_total, categoria, "
                + "id_politica) VALUES (" + idSolicitante + ", " + puntaje + ", '" + categoria + "', " + idPolitica
                + ") RETURNING id_evaluacion");
    }

    private static void insertarDetalle(Connection connection, long idEvaluacion, int puntaje) throws SQLException {
        ejecutar(connection, "INSERT INTO detalle_evaluacion (id_evaluacion, id_regla, operador_aplicado, "
                + "valor_condicion_aplicado, puntaje_regla_aplicado, condicion_cumplida, puntaje_obtenido) VALUES ("
                + idEvaluacion + ", " + idRegla + ", '>=', '3000000', " + puntaje + ", true, " + puntaje + ")");
    }

    private static long insertarDevolviendoId(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
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

    private static long contarEvaluaciones() throws SQLException {
        try (Connection connection = conectar()) {
            return Long.parseLong(consultarTexto(connection, "SELECT count(*)::text FROM evaluacion"));
        }
    }

    private static void enTransaccionRevertida(AccionSql accion) throws SQLException {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                accion.ejecutar(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    @FunctionalInterface
    private interface AccionSql {
        void ejecutar(Connection connection) throws SQLException;
    }
}
