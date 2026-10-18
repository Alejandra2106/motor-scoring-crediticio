package com.crediticio.scoring.infrastructure.output;

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
 * HU11 CA11 (P2): aplicación de la migración V9 (src/main/resources/db/migration, validada con esta
 * prueba antes de su promoción), que hace inmutables en la base las versiones publicadas de la
 * política de clasificación y sus rangos, en un esquema aislado y temporal.
 *
 * Antes de abrir cualquier conexión exige, por igualdad exacta de texto, que -Dit.base-dedicada sea
 * motor_scoring_it y que -Dspring.datasource.url sea jdbc:postgresql://localhost:5432/motor_scoring_it
 * (nunca usa DB_URL). Después comprueba con current_database() que la conexión llega a esa base y
 * exige que el esquema no exista. Aplica V1-V8, publica una política (anterior a V9), aplica V9 y
 * verifica. No toca el esquema public ni sus datos.
 *
 * Limpieza: solo elimina hu11_v9_it si la base demuestra que lo creó esta ejecución (mismo OID
 * registrado tras crearlo y exactamente una marca de creación de Flyway, con el historial bloqueado).
 * Si no, no elimina nada y hace fallar la ejecución indicando el motivo. DROP SCHEMA no dispara los
 * triggers de V9.
 *
 * Al terminar en "IT", Surefire no la ejecuta con `./mvnw test`. Ejecución manual (requiere
 * autorización expresa para usar motor_scoring_it), desde la raíz del proyecto:
 *   mvnw.cmd -B test "-Dtest=PoliticaClasificacionV9MigracionIT"
 *     "-Dit.base-dedicada=motor_scoring_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
 * con DB_USERNAME y DB_PASSWORD en el entorno.
 */
class PoliticaClasificacionV9MigracionIT {

    private static final String ESQUEMA = "hu11_v9_it";
    /** Únicos valores admitidos; se comparan por igualdad exacta antes de abrir cualquier conexión. */
    private static final String BASE_AUTORIZADA = "motor_scoring_it";
    private static final String URL_AUTORIZADA = "jdbc:postgresql://localhost:5432/" + BASE_AUTORIZADA;
    private static final String UBICACION_MIGRACIONES = "classpath:db/migration";
    private static final String UBICACION_PROPUESTA = "filesystem:database/propuestas/hu11-v9";
    private static final String OBJECT_NOT_IN_PREREQUISITE_STATE = "55000";

    private static String url;
    private static String usuario;
    private static String clave;
    /** Solo habilita el search_path del esquema; la limpieza no se basa en esta marca. */
    private static boolean esquemaCreadoPorLaPrueba;
    /** OID del esquema leído justo después de que Flyway lo creara; null si no llegó a registrarse. */
    private static Long oidEsquemaCreado;

    private static long idPolitica;
    private static long idSolicitante;

    @BeforeAll
    static void prepararEsquemaConPoliticaPublicadaYAplicarV9() throws SQLException {
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
        flyway("8").migrate();
        oidEsquemaCreado = registrarEsquemaCreadoPorFlyway();
        try (Connection connection = conectar()) {
            idPolitica = publicarPolitica(connection, 1);
            idSolicitante = insertarDevolviendoId(connection,
                    "INSERT INTO solicitante (nombre_completo, numero_documento, ingresos_mensuales, deudas_mensuales, "
                            + "numero_moras, historial_crediticio, antiguedad_laboral) "
                            + "VALUES ('Solicitante HU11', '900000004', 4000000, 1200000, 0, 'BUENO', 2) "
                            + "RETURNING id_solicitante");
        }

        flyway("9").migrate();
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

    @Test
    void v9DebeQuedarAplicadaConSusCuatroTriggers() throws SQLException {
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT string_agg(version, ',' ORDER BY installed_rank) FROM "
                    + ESQUEMA + ".flyway_schema_history WHERE success")).endsWith(",8,9");
            assertThat(consultarTexto(connection, "SELECT string_agg(t.tgname, ',' ORDER BY t.tgname) FROM pg_trigger t "
                    + "JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_namespace n ON n.oid = c.relnamespace "
                    + "WHERE n.nspname = '" + ESQUEMA + "' AND NOT t.tgisinternal "
                    + "AND c.relname IN ('politica_clasificacion', 'rango_clasificacion')"))
                    .isEqualTo("tg_politica_clasificacion_inmutable,tg_politica_clasificacion_sin_truncate,"
                            + "tg_rango_clasificacion_inmutable,tg_rango_clasificacion_sin_truncate");
        }
    }

    @Test
    void unaPoliticaPublicadaNoDebePoderModificarse() throws SQLException {
        assertRechazadoPorInmutabilidad("UPDATE politica_clasificacion SET fecha_publicacion = fecha_publicacion "
                + "WHERE id_politica = " + idPolitica);
    }

    @Test
    void unaPoliticaPublicadaNoDebePoderBorrarse() throws SQLException {
        assertRechazadoPorInmutabilidad("DELETE FROM politica_clasificacion WHERE id_politica = " + idPolitica);
    }

    @Test
    void lasPoliticasNoDebenPoderVaciarseConTruncate() throws SQLException {
        // CASCADE: sin él, la clave foránea de rango_clasificacion haría fallar el TRUNCATE antes del trigger
        assertRechazadoPorInmutabilidad("TRUNCATE politica_clasificacion CASCADE");
    }

    @Test
    void unRangoPublicadoNoDebePoderModificarse() throws SQLException {
        assertRechazadoPorInmutabilidad("UPDATE rango_clasificacion SET limite_superior = limite_superior "
                + "WHERE id_politica = " + idPolitica);
    }

    @Test
    void unRangoPublicadoNoDebePoderBorrarse() throws SQLException {
        assertRechazadoPorInmutabilidad("DELETE FROM rango_clasificacion WHERE id_politica = " + idPolitica);
    }

    @Test
    void losRangosNoDebenPoderVaciarseConTruncate() throws SQLException {
        assertRechazadoPorInmutabilidad("TRUNCATE rango_clasificacion");
    }

    @Test
    void debeSeguirPudiendoPublicarseUnaVersionNuevaYCambiarseLaVigente() throws SQLException {
        long idNueva;
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            idNueva = publicarPolitica(connection,
                    Integer.parseInt(consultarTexto(connection, "SELECT max(version) + 1 FROM politica_clasificacion")));
            ejecutar(connection, "UPDATE configuracion_clasificacion_actual SET id_politica = " + idNueva
                    + " WHERE id_configuracion = 1");
            connection.commit();
        }
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT count(*)::text FROM rango_clasificacion WHERE id_politica = "
                    + idNueva)).isEqualTo("3");
            assertThat(consultarTexto(connection, "SELECT id_politica::text FROM configuracion_clasificacion_actual"))
                    .isEqualTo(String.valueOf(idNueva));
        }
    }

    @Test
    void unaEvaluacionDebeSeguirPudiendoReferenciarUnaPoliticaPublicada() throws SQLException {
        long idEvaluacion;
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            // puntaje 0 sin detalles (suma 0, trigger A de V8); 0 pertenece al rango BAJO (trigger B)
            idEvaluacion = insertarDevolviendoId(connection, "INSERT INTO evaluacion (id_solicitante, score_total, "
                    + "categoria, id_politica) VALUES (" + idSolicitante + ", 0, 'BAJO', " + idPolitica
                    + ") RETURNING id_evaluacion");
            connection.commit();
        }
        try (Connection connection = conectar()) {
            assertThat(consultarTexto(connection, "SELECT concat_ws('|', score_total, categoria, id_politica) FROM "
                    + "evaluacion WHERE id_evaluacion = " + idEvaluacion)).isEqualTo("0|BAJO|" + idPolitica);
        }
    }

    // ---------------------------------------------------------------------------------------

    private static void assertRechazadoPorInmutabilidad(String sql) throws SQLException {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                assertThatThrownBy(() -> ejecutar(connection, sql)).as(sql)
                        .isInstanceOfSatisfying(SQLException.class, ex -> {
                            assertThat(ex.getSQLState()).isEqualTo(OBJECT_NOT_IN_PREREQUISITE_STATE);
                            assertThat(ex.getMessage()).contains("son inmutables");
                        });
            } finally {
                connection.rollback();
            }
        }
    }

    /** Política con rangos BAJO [-100, 29], MEDIO [30, 79] y ALTO [80, 130] (ejemplo del Gherkin de HU11). */
    private static long publicarPolitica(Connection connection, int version) throws SQLException {
        long id = insertarDevolviendoId(connection, "INSERT INTO politica_clasificacion (version, publicado_por) "
                + "VALUES (" + version + ", '" + UUID.randomUUID() + "') RETURNING id_politica");
        ejecutar(connection, "INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, limite_superior) "
                + "VALUES (" + id + ", 'BAJO', -100, 29), (" + id + ", 'MEDIO', 30, 79), (" + id + ", 'ALTO', 80, 130)");
        return id;
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
