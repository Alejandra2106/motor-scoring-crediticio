package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.config.JwtConfig;
import com.crediticio.support.ClavesRsaPrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU12 de punta a punta sobre H2 en memoria (perfil test, sin PostgreSQL ni Flyway): la API, los
 * casos de uso, la persistencia JPA real y la consulta nativa de la versión de política. Verifica
 * que los rechazos 409 no persisten nada y que una evaluación conserva su categoría y la referencia
 * exacta a la versión de política con la que se clasificó, aunque después se publique otra.
 *
 * No sustituye a la validación sobre PostgreSQL: el esquema lo crea Hibernate a partir de las
 * entidades, de modo que aquí no existen las restricciones ni los triggers de la migración V8.
 * Las pruebas comparten estado y se ejecutan en orden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClasificacionEvaluacionFlujoTest {

    private static final String RUTA_EVALUACIONES = "/api/v1/evaluaciones";
    /** Rangos de ejemplo del Gherkin de HU11; un puntaje de 50 es MEDIO. */
    private static final String POLITICA_1 = """
            {"rangos": [
              {"categoria": "BAJO", "limiteInferior": -100, "limiteSuperior": 29},
              {"categoria": "MEDIO", "limiteInferior": 30, "limiteSuperior": 79},
              {"categoria": "ALTO", "limiteInferior": 80, "limiteSuperior": 130}
            ]}""";
    /** Segunda versión válida en la que un puntaje de 50 es BAJO. */
    private static final String POLITICA_2 = """
            {"rangos": [
              {"categoria": "BAJO", "limiteInferior": -100, "limiteSuperior": 59},
              {"categoria": "MEDIO", "limiteInferior": 60, "limiteSuperior": 79},
              {"categoria": "ALTO", "limiteInferior": 80, "limiteSuperior": 130}
            ]}""";

    /**
     * Con @TestInstance(PER_CLASS) el contexto de Spring se carga antes de que JUnit inyecte un
     * @TempDir estático, así que el directorio de la clave pública de prueba se crea aquí mismo.
     */
    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("scoring.jwt.clave-publica", () -> {
            Path claves;
            try {
                claves = Files.createTempDirectory("hu12-flujo-claves");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            claves.toFile().deleteOnExit();
            Path clave = ClavesRsaPrueba.escribirClavePublica(ClavesRsaPrueba.generar(), claves);
            clave.toFile().deleteOnExit();
            return "file:" + clave;
        });
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private long idSolicitante;
    private long idPolitica1;
    private long idEvaluacionConPolitica1;

    private static RequestPostProcessor conRol(String rol) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim(JwtConfig.CLAIM_ROL, rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    /**
     * El esquema de H2 lo crea Hibernate a partir de las entidades, sin lo que en PostgreSQL aportan
     * las migraciones. Aquí se replica solo lo imprescindible: los DEFAULT CURRENT_TIMESTAMP de las
     * columnas que las entidades no insertan (V1, V2, V4 y V5) y la fila única de configuración de
     * V6, sin política vigente. Los datos de scoring se crean por la propia API.
     */
    @BeforeAll
    void prepararDatos() throws Exception {
        ejecutar("ALTER TABLE solicitante ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP");
        ejecutar("ALTER TABLE riesgo ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP");
        ejecutar("ALTER TABLE regla_scoring ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP");
        ejecutar("ALTER TABLE evaluacion ALTER COLUMN fecha_evaluacion SET DEFAULT CURRENT_TIMESTAMP");
        ejecutar("INSERT INTO configuracion_clasificacion_actual (id_configuracion, id_politica) VALUES (1, NULL)");

        long idRiesgo = crearYObtenerId("/api/v1/variables-riesgo", JwtConfig.ROL_ADMINISTRADOR, """
                {"variable": "INGRESOS_MENSUALES", "descripcion": "Ingresos mensuales del solicitante"}""",
                "$.idVariableRiesgo");
        crearYObtenerId("/api/v1/reglas-scoring", JwtConfig.ROL_ADMINISTRADOR, """
                {"idRiesgo": %d, "operador": ">=", "valorCondicion": "3000000", "puntaje": 50}""".formatted(idRiesgo),
                "$.idRegla");
        idSolicitante = crearYObtenerId("/api/v1/solicitantes", JwtConfig.ROL_ANALISTA, """
                {"nombreCompleto": "Solicitante HU12", "numeroDocumento": "900000003",
                 "ingresosMensuales": 4000000, "deudasMensuales": 1200000, "numeroMoras": 0,
                 "historialCrediticio": "BUENO", "antiguedadLaboral": 2}""", "$.idSolicitante");
    }

    @Test
    @Order(1)
    void sinPoliticaVigenteDebeResponder409SinPersistirNada() throws Exception {
        mockMvc.perform(calcular())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POLITICA_VIGENTE_NO_DISPONIBLE"));

        assertThat(contar("evaluacion")).isZero();
        assertThat(contar("detalle_evaluacion")).isZero();
    }

    @Test
    @Order(2)
    void debePersistirElPuntajeLaCategoriaYLaReferenciaALaVersionUtilizada() throws Exception {
        idPolitica1 = crearYObtenerId("/api/v1/politicas-clasificacion", JwtConfig.ROL_ADMINISTRADOR, POLITICA_1,
                "$.idPolitica");

        String respuesta = mockMvc.perform(calcular())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreTotal").value(50))
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.idPolitica").value(idPolitica1))
                .andExpect(jsonPath("$.versionPolitica").value(1))
                .andExpect(jsonPath("$.detalles[0].puntajeObtenido").value(50))
                .andReturn().getResponse().getContentAsString();
        idEvaluacionConPolitica1 = ((Number) JsonPath.read(respuesta, "$.idEvaluacion")).longValue();

        assertThat(filaDeEvaluacion(idEvaluacionConPolitica1)).isEqualTo("50|MEDIO|" + idPolitica1);
        assertThat(texto("SELECT SUM(puntaje_obtenido) FROM detalle_evaluacion WHERE id_evaluacion = "
                + idEvaluacionConPolitica1)).isEqualTo("50");
    }

    @Test
    @Order(3)
    void laConsultaPorIdDebeRecuperarLoRegistradoConLaVersionDePolitica() throws Exception {
        mockMvc.perform(get(RUTA_EVALUACIONES + "/" + idEvaluacionConPolitica1).with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreTotal").value(50))
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.idPolitica").value(idPolitica1))
                .andExpect(jsonPath("$.versionPolitica").value(1));
    }

    @Test
    @Order(4)
    void unaNuevaPoliticaNoDebeReclasificarLasEvaluacionesExistentes() throws Exception {
        crearYObtenerId("/api/v1/politicas-clasificacion", JwtConfig.ROL_ADMINISTRADOR, POLITICA_2, "$.idPolitica");

        mockMvc.perform(get(RUTA_EVALUACIONES + "/" + idEvaluacionConPolitica1).with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.versionPolitica").value(1));
        assertThat(filaDeEvaluacion(idEvaluacionConPolitica1)).isEqualTo("50|MEDIO|" + idPolitica1);

        mockMvc.perform(calcular())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("BAJO"))
                .andExpect(jsonPath("$.versionPolitica").value(2));
    }

    @Test
    @Order(5)
    void unaEvaluacionInexistenteDebeResponder404() throws Exception {
        mockMvc.perform(get(RUTA_EVALUACIONES + "/999999").with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @Order(6)
    void unPuntajeSinClasificacionDebeResponder409SinPersistirNada() throws Exception {
        long idRiesgoMoras = crearYObtenerId("/api/v1/variables-riesgo", JwtConfig.ROL_ADMINISTRADOR, """
                {"variable": "NUMERO_MORAS", "descripcion": "Número de moras del solicitante"}""",
                "$.idVariableRiesgo");
        crearYObtenerId("/api/v1/reglas-scoring", JwtConfig.ROL_ADMINISTRADOR, """
                {"idRiesgo": %d, "operador": "=", "valorCondicion": "0", "puntaje": 100}""".formatted(idRiesgoMoras),
                "$.idRegla");
        long evaluaciones = contar("evaluacion");
        long detalles = contar("detalle_evaluacion");

        // 50 + 100 = 150, fuera del intervalo de -100 a 130 que cubre toda política
        mockMvc.perform(calcular())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PUNTAJE_SIN_CLASIFICACION"));

        assertThat(contar("evaluacion")).isEqualTo(evaluaciones);
        assertThat(contar("detalle_evaluacion")).isEqualTo(detalles);
    }

    private MockHttpServletRequestBuilder calcular() {
        return post(RUTA_EVALUACIONES).with(conRol(JwtConfig.ROL_ANALISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idSolicitante\": " + idSolicitante + "}");
    }

    private long crearYObtenerId(String ruta, String rol, String cuerpo, String campoId) throws Exception {
        String respuesta = mockMvc.perform(post(ruta).with(conRol(rol))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, campoId)).longValue();
    }

    private String filaDeEvaluacion(long idEvaluacion) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT score_total, categoria, id_politica FROM evaluacion WHERE id_evaluacion = ?")) {
            statement.setLong(1, idEvaluacion);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("evaluación " + idEvaluacion).isTrue();
                return resultSet.getInt(1) + "|" + resultSet.getString(2) + "|" + resultSet.getLong(3);
            }
        }
    }

    private long contar(String tabla) throws SQLException {
        return Long.parseLong(texto("SELECT COUNT(*) FROM " + tabla));
    }

    private String texto(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private void ejecutar(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
