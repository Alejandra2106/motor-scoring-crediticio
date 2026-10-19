package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.ResultadoReclamo;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU14: sentencias de la outbox (TrabajoRecomendacionAdapter) contra PostgreSQL con V1–V11 en un esquema aislado y
 * temporal: reclamo con concesión, barrera por token, éxito, error transitorio con espera, error permanente y
 * agotamiento de concesiones. Cada escenario usa su propia evaluación y la deja en un estado terminal o con una
 * espera futura, para que no interfiera con los siguientes.
 *
 * Mismas protecciones y limpieza que TrabajoRecomendacionV11MigracionIT. Al terminar en "IT", Surefire no la
 * ejecuta con `./mvnw test`. Ejecución manual (requiere autorización expresa para usar motor_scoring_it):
 *   mvnw.cmd -B test "-Dtest=TrabajoRecomendacionAdapterIT" "-Dit.base-dedicada=motor_scoring_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
 * con DB_USERNAME y DB_PASSWORD en el entorno.
 */
class TrabajoRecomendacionAdapterIT {

    private static final String ESQUEMA = "hu14_trabajo_it";
    private static final String BASE_AUTORIZADA = "motor_scoring_it";
    private static final String URL_AUTORIZADA = "jdbc:postgresql://localhost:5432/" + BASE_AUTORIZADA;
    private static final Duration CONCESION = Duration.ofSeconds(30);

    private static String url;
    private static String usuario;
    private static String clave;
    private static boolean esquemaCreadoPorLaPrueba;
    private static Long oidEsquemaCreado;
    private static JdbcTemplate jdbcTemplate;
    private static TrabajoRecomendacionAdapter adaptador;
    private static long idSolicitante;
    private static long idPolitica;

    @BeforeAll
    static void prepararEsquema() throws SQLException {
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
        try (Connection connection = DriverManager.getConnection(url, usuario, clave);
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
        Flyway.configure().dataSource(url, usuario, clave).schemas(ESQUEMA).locations("classpath:db/migration")
                .cleanDisabled(true).load().migrate();
        oidEsquemaCreado = registrarEsquemaCreadoPorFlyway();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(url + "?currentSchema=" + ESQUEMA, usuario,
                clave);
        jdbcTemplate = new JdbcTemplate(dataSource);
        adaptador = new TrabajoRecomendacionAdapter(null, jdbcTemplate, new DataSourceTransactionManager(dataSource));

        idSolicitante = jdbcTemplate.queryForObject("INSERT INTO solicitante (nombre_completo, numero_documento, "
                + "ingresos_mensuales, deudas_mensuales, numero_moras, historial_crediticio, antiguedad_laboral) "
                + "VALUES ('Solicitante HU14', '900000301', 4000000, 1200000, 0, 'BUENO', 2) RETURNING id_solicitante",
                Long.class);
        idPolitica = jdbcTemplate.queryForObject("INSERT INTO politica_clasificacion (version, publicado_por) VALUES "
                + "(1, '" + UUID.randomUUID() + "') RETURNING id_politica", Long.class);
        jdbcTemplate.update("INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, limite_superior) "
                + "VALUES (?, 'BAJO', -100, 29), (?, 'MEDIO', 30, 79), (?, 'ALTO', 80, 130)", idPolitica, idPolitica,
                idPolitica);
    }

    @AfterAll
    static void eliminarSoloElEsquemaCreadoPorLaPrueba() {
        if (!esquemaCreadoPorLaPrueba) {
            return;
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
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            motivo = "falló la comprobación o el DROP; transacción revertida (SQLState " + ex.getSQLState() + ")";
        }
        throw new AssertionError("No se eliminó el esquema " + ESQUEMA + ": " + motivo
                + ". Revíselo manualmente antes de volver a ejecutar la prueba.");
    }

    @Test
    void reclamoBarreraYExito() throws Exception {
        UUID inicial = UUID.randomUUID();
        long id = evaluacionPendienteConConcesion(inicial, 1);

        assertThat(adaptador.reclamarSiguiente(10, UUID.randomUUID(), CONCESION))
                .as("con la concesión inicial vigente no se reclama").isInstanceOf(ResultadoReclamo.Ninguno.class);
        Thread.sleep(1500);
        UUID nuevo = UUID.randomUUID();
        assertThat(adaptador.reclamarSiguiente(10, nuevo, CONCESION))
                .isEqualTo(new ResultadoReclamo.Reclamado(id, nuevo, 2));

        assertThat(adaptador.registrarExito(id, inicial)).as("token antiguo").isFalse();
        assertThat(adaptador.registrarExito(id, nuevo)).isTrue();
        assertThat(estado(id)).isEqualTo("COMPLETADA|PROCESADO|2|2|-");
    }

    @Test
    void errorTransitorioConEsperaYErrorPermanente() throws Exception {
        UUID token = UUID.randomUUID();
        long id = evaluacionPendienteConConcesion(token, 30);

        assertThat(adaptador.registrarErrorTransitorio(id, token, Duration.ofSeconds(1), "SIN_CONEXION", "detalle"))
                .isTrue();
        assertThat(estado(id)).isEqualTo("PENDIENTE|PENDIENTE|1|1|SIN_CONEXION");
        assertThat(adaptador.reclamarSiguiente(10, UUID.randomUUID(), CONCESION))
                .as("la espera todavía no venció").isInstanceOf(ResultadoReclamo.Ninguno.class);
        Thread.sleep(1300);
        UUID reclamo = UUID.randomUUID();
        assertThat(adaptador.reclamarSiguiente(10, reclamo, CONCESION))
                .isEqualTo(new ResultadoReclamo.Reclamado(id, reclamo, 2));

        assertThat(adaptador.registrarErrorPermanente(id, reclamo, "RECHAZO_FUNCIONAL", "HTTP 409")).isTrue();
        assertThat(estado(id)).isEqualTo("PENDIENTE|ERROR_PERMANENTE|2|2|RECHAZO_FUNCIONAL");
    }

    @Test
    void unaConcesionVencidaConLosIntentosAgotadosPasaAErrorPermanente() throws Exception {
        long id = evaluacionPendienteConConcesion(UUID.randomUUID(), 1);
        Thread.sleep(1500);

        assertThat(adaptador.reclamarSiguiente(1, UUID.randomUUID(), CONCESION))
                .isEqualTo(new ResultadoReclamo.Agotado(id));
        assertThat(estado(id)).isEqualTo("PENDIENTE|ERROR_PERMANENTE|1|1|CONCESIONES_AGOTADAS");
    }

    // ---------------------------------------------------------------------------------------

    /** Evaluación PENDIENTE con su trabajo y la concesión del intento en línea, en una transacción (V11). */
    private static long evaluacionPendienteConConcesion(UUID token, int segundosDeConcesion) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, usuario, clave)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + ESQUEMA);
                long id;
                try (ResultSet resultSet = statement.executeQuery("INSERT INTO evaluacion (id_solicitante, score_total, "
                        + "categoria, id_politica, estado) VALUES (" + idSolicitante + ", 0, 'BAJO', " + idPolitica
                        + ", 'PENDIENTE') RETURNING id_evaluacion")) {
                    resultSet.next();
                    id = resultSet.getLong(1);
                }
                statement.execute("INSERT INTO trabajo_recomendacion (id_evaluacion, intentos, intentos_totales, "
                        + "proximo_intento, id_concesion, concesion_hasta) VALUES (" + id + ", 1, 1, clock_timestamp(), '"
                        + token + "', clock_timestamp() + make_interval(secs => " + segundosDeConcesion + "))");
                connection.commit();
                return id;
            }
        }
    }

    private static String estado(long id) {
        return jdbcTemplate.queryForObject("SELECT concat_ws('|', e.estado, t.estado_trabajo, t.intentos, "
                + "t.intentos_totales, coalesce(t.codigo_ultimo_error, '-')) FROM evaluacion e "
                + "JOIN trabajo_recomendacion t ON t.id_evaluacion = e.id_evaluacion WHERE e.id_evaluacion = ?",
                String.class, id);
    }

    private static String motivoParaNoEliminarElEsquema(Connection connection) throws SQLException {
        if (oidEsquemaCreado == null) {
            return "no se registró el OID del esquema tras su creación";
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + ESQUEMA + ".flyway_schema_history IN ACCESS EXCLUSIVE MODE");
        }
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
}
