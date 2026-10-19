package com.crediticio.scoring.infrastructure.output;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU11 CA14: compatibilidad de V7 con versiones históricas sin autor. Con V7 aplicada ya no se puede
 * insertar una política sin autor, así que la fila histórica solo puede existir si se crea antes de
 * la migración. Por eso esta prueba usa un esquema aislado y temporal de la base dedicada: aplica
 * V1-V6, inserta una política histórica sin autor, aplica V7 y verifica que la fila se conserva, que
 * la restricción queda NOT VALID y que una fila nueva sin autor se rechaza. Al terminar elimina solo
 * ese esquema. No toca el esquema public ni sus datos.
 *
 * Ejecución (requiere autorización expresa para usar motor_scoring_it):
 *   mvnw.cmd -B test "-Dtest=PoliticaClasificacionV7MigracionIT" "-Dsurefire.failIfNoSpecifiedTests=false"
 *     "-Dhu11.it.base-dedicada=motor_scoring_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
 * con DB_USERNAME y DB_PASSWORD en el entorno.
 */
class PoliticaClasificacionV7MigracionIT {

    private static final String SUFIJO_BASE_DEDICADA = "_it";
    private static final String ESQUEMA = "hu11_v7_historico_it";
    private static final String RESTRICCION_AUTOR = "ck_politica_clasificacion_publicado_por";

    private String url;
    private String usuario;
    private String clave;
    private boolean esquemaCreadoPorLaPrueba;

    @BeforeEach
    void verificarBaseDedicadaYEsquemaInexistente() throws SQLException {
        String baseAutorizada = System.getProperty("hu11.it.base-dedicada");
        url = System.getProperty("spring.datasource.url");
        usuario = System.getenv("DB_USERNAME");
        clave = System.getenv("DB_PASSWORD");
        assertThat(baseAutorizada).as("propiedad hu11.it.base-dedicada").isNotBlank().endsWith(SUFIJO_BASE_DEDICADA);
        assertThat(url).as("propiedad spring.datasource.url").isNotBlank();
        assertThat(usuario).as("DB_USERNAME").isNotBlank();
        assertThat(clave).as("DB_PASSWORD").isNotBlank();
        try (Connection connection = DriverManager.getConnection(url, usuario, clave);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT current_database(), "
                                + "EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getString(1)).as("base de datos de la conexión").isEqualTo(baseAutorizada);
                assertThat(resultSet.getBoolean(2))
                        .as("el esquema " + ESQUEMA + " ya existe; no se reutiliza ni se elimina").isFalse();
            }
        }
    }

    @AfterEach
    void eliminarSoloElEsquemaCreadoPorLaPrueba() throws SQLException {
        if (!esquemaCreadoPorLaPrueba) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(url, usuario, clave);
                Statement statement = connection.createStatement()) {
            // el esquema no existía antes de la prueba (verificado en @BeforeEach)
            statement.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        }
    }

    @Test
    void v7DebeConservarLasPoliticasHistoricasSinAutorYExigirAutorEnLasNuevas() throws SQLException {
        Timestamp instante = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));

        esquemaCreadoPorLaPrueba = true;
        flyway("6").migrate();
        long idHistorica;
        try (Connection connection = DriverManager.getConnection(url, usuario, clave);
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + ESQUEMA + ".politica_clasificacion "
                                + "(version, fecha_creacion, fecha_publicacion) VALUES (1, ?, ?) "
                                + "RETURNING id_politica")) {
            statement.setTimestamp(1, instante);
            statement.setTimestamp(2, instante);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                idHistorica = resultSet.getLong(1);
            }
        }

        flyway(null).migrate();

        try (Connection connection = DriverManager.getConnection(url, usuario, clave)) {
            assertThat(consultarEntero(connection, "SELECT count(*) FROM " + ESQUEMA
                    + ".flyway_schema_history WHERE version = '7' AND success")).isEqualTo(1);

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT version, fecha_creacion, fecha_publicacion, publicado_por FROM " + ESQUEMA
                            + ".politica_clasificacion WHERE id_politica = ?")) {
                statement.setLong(1, idHistorica);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).as("la política histórica debe conservarse").isTrue();
                    assertThat(resultSet.getInt(1)).isEqualTo(1);
                    assertThat(resultSet.getTimestamp(2)).isEqualTo(instante);
                    assertThat(resultSet.getTimestamp(3)).isEqualTo(instante);
                    assertThat(resultSet.getObject(4)).as("autor de la política histórica").isNull();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT c.convalidated FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace "
                            + "WHERE n.nspname = ? AND c.conname = ?")) {
                statement.setString(1, ESQUEMA);
                statement.setString(2, RESTRICCION_AUTOR);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).as(RESTRICCION_AUTOR).isTrue();
                    assertThat(resultSet.getBoolean(1)).as("la restricción debe quedar NOT VALID").isFalse();
                }
            }

            connection.setAutoCommit(false);
            try {
                assertThatThrownBy(() -> insertarPolitica(connection, 2, instante, null))
                        .isInstanceOfSatisfying(SQLException.class, ex -> {
                            assertThat(ex.getSQLState()).isEqualTo("23514");
                            assertThat(ex.getMessage()).contains(RESTRICCION_AUTOR);
                        });
                connection.rollback();
                insertarPolitica(connection, 2, instante, UUID.randomUUID());
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }

    private Flyway flyway(String version) {
        var configuracion = Flyway.configure()
                .dataSource(url, usuario, clave)
                .schemas(ESQUEMA)
                .locations("classpath:db/migration")
                .cleanDisabled(true);
        if (version != null) {
            configuracion = configuracion.target(version);
        }
        return configuracion.load();
    }

    private void insertarPolitica(Connection connection, int version, Timestamp instante, UUID autor)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + ESQUEMA + ".politica_clasificacion "
                        + "(version, fecha_creacion, fecha_publicacion, publicado_por) VALUES (?, ?, ?, ?)")) {
            statement.setInt(1, version);
            statement.setTimestamp(2, instante);
            statement.setTimestamp(3, instante);
            statement.setObject(4, autor, java.sql.Types.OTHER);
            statement.executeUpdate();
        }
    }

    private int consultarEntero(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
