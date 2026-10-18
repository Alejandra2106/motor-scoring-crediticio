package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.config.JwtConfig;
import com.crediticio.support.ClavesRsaPrueba;
import com.crediticio.support.ProteccionBaseIntegracion;
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
import org.springframework.test.context.ContextConfiguration;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU12 de punta a punta contra PostgreSQL real, en un esquema dedicado y temporal con la migración
 * V8 (src/main/resources/db/migration): cálculo, clasificación, persistencia, rechazos 409 sin
 * persistencia, consulta por id y autorización por rol.
 *
 * {@link ProteccionBaseIntegracion} valida la configuración sin conectarse antes de crear el
 * contexto (base dedicada exacta, URL explícita, esquema dedicado coherente con currentSchema y
 * spring.flyway.schemas; las migraciones propuestas solo se admiten con esquema dedicado). Además,
 * antes de la primera prueba se exige que el esquema esté recién creado (sin datos de trabajo ni
 * política vigente). Las pruebas se ejecutan en orden porque comparten ese estado. La prueba no
 * elimina el esquema: se borra después con el comando indicado en database/propuestas/hu12/README.md.
 *
 * Al terminar en "IT", Surefire no la ejecuta con `./mvnw test`. Ejecución manual (requiere
 * autorización expresa; crea y usa el esquema hu12_clasificacion_it de motor_scoring_it):
 *   mvnw.cmd -B test "-Dtest=ClasificacionEvaluacionIT"
 *     "-Dspring.profiles.active=it"
 *     "-Dit.base-dedicada=motor_scoring_it" "-Dit.esquema-dedicado=hu12_clasificacion_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it?currentSchema=hu12_clasificacion_it"
 *     "-Dspring.flyway.schemas=hu12_clasificacion_it"
 * con DB_USERNAME y DB_PASSWORD en el entorno. Sin -Dsurefire.failIfNoSpecifiedTests=false: un
 * nombre de prueba incorrecto debe hacer fallar Maven. Resultado esperado: "Tests run: 7".
 */
@SpringBootTest(properties = "scoring.jwt.emisor=auth-api-prueba")
@AutoConfigureMockMvc
@ActiveProfiles("it")
@ContextConfiguration(initializers = ProteccionBaseIntegracion.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClasificacionEvaluacionIT {

    private static final String ESQUEMA = "hu12_clasificacion_it";
    private static final String RUTA_EVALUACIONES = "/api/v1/evaluaciones";
    private static final String RUTA_POLITICAS = "/api/v1/politicas-clasificacion";
    /** Puntaje de la regla base: 50 es MEDIO en la política 1 y BAJO en la política 2. */
    private static final String POLITICA_1 = """
            {"rangos": [
              {"categoria": "ALTO", "limiteInferior": -100, "limiteSuperior": 39},
              {"categoria": "MEDIO", "limiteInferior": 40, "limiteSuperior": 79},
              {"categoria": "BAJO", "limiteInferior": 80, "limiteSuperior": 130}
            ]}""";
    private static final String POLITICA_2 = """
            {"rangos": [
              {"categoria": "ALTO", "limiteInferior": -100, "limiteSuperior": 29},
              {"categoria": "MEDIO", "limiteInferior": 30, "limiteSuperior": 49},
              {"categoria": "BAJO", "limiteInferior": 50, "limiteSuperior": 130}
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
                claves = Files.createTempDirectory("hu12-it-claves");
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
    private long idEvaluacionConPolitica1;

    private static RequestPostProcessor conRol(String rol) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim(JwtConfig.CLAIM_ROL, rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @BeforeAll
    void exigirEsquemaDedicadoRecienCreadoYPrepararDatos() throws SQLException {
        assertThat(System.getProperty(ProteccionBaseIntegracion.PROPIEDAD_ESQUEMA_DEDICADO))
                .as("esquema dedicado de esta prueba").isEqualTo(ESQUEMA);
        assertThat(texto("SELECT current_schema()")).isEqualTo(ESQUEMA);
        for (String tabla : new String[] {"solicitante", "riesgo", "regla_scoring", "evaluacion",
                "detalle_evaluacion", "politica_clasificacion"}) {
            assertThat(numero("SELECT count(*) FROM " + tabla))
                    .as("el esquema debe estar recién creado: la tabla %s tiene datos", tabla).isZero();
        }
        assertThat(texto("SELECT coalesce(id_politica::text, '') FROM configuracion_clasificacion_actual"))
                .as("no debe haber política vigente").isEmpty();

        idSolicitante = numero("INSERT INTO solicitante (nombre_completo, numero_documento, ingresos_mensuales, "
                + "deudas_mensuales, numero_moras, historial_crediticio, antiguedad_laboral) VALUES "
                + "('Solicitante HU12', '900000002', 4000000, 1200000, 0, 'BUENO', 2) RETURNING id_solicitante");
        long idRiesgo = numero("INSERT INTO riesgo (variable, descripcion) VALUES ('INGRESOS_MENSUALES', "
                + "'Ingresos mensuales del solicitante') RETURNING id_riesgo");
        numero("INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) VALUES (" + idRiesgo
                + ", '>=', '3000000', 50) RETURNING id_regla");
    }

    @Test
    @Order(1)
    void sinPoliticaVigenteDebeResponder409SinPersistirLaEvaluacion() throws Exception {
        long antes = numero("SELECT count(*) FROM evaluacion");

        mockMvc.perform(calcular().with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POLITICA_VIGENTE_NO_DISPONIBLE"));

        assertThat(numero("SELECT count(*) FROM evaluacion")).isEqualTo(antes);
        assertThat(numero("SELECT count(*) FROM detalle_evaluacion")).isZero();
    }

    @Test
    @Order(2)
    void debeClasificarYPersistirPuntajeCategoriaYVersionDePolitica() throws Exception {
        publicar(POLITICA_1);
        long idPolitica1 = numero("SELECT id_politica FROM politica_clasificacion WHERE version = 1");

        String respuesta = mockMvc.perform(calcular().with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreTotal").value(50))
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.idPolitica").value(idPolitica1))
                .andExpect(jsonPath("$.versionPolitica").value(1))
                .andReturn().getResponse().getContentAsString();
        idEvaluacionConPolitica1 = Long.parseLong(respuesta.replaceAll(".*\"idEvaluacion\":(\\d+).*", "$1"));

        assertThat(texto("SELECT concat_ws('|', score_total, categoria, id_politica) FROM evaluacion "
                + "WHERE id_evaluacion = " + idEvaluacionConPolitica1)).isEqualTo("50|MEDIO|" + idPolitica1);
        assertThat(numero("SELECT sum(puntaje_obtenido) FROM detalle_evaluacion WHERE id_evaluacion = "
                + idEvaluacionConPolitica1)).isEqualTo(50);
    }

    @Test
    @Order(3)
    void laConsultaPorIdDebeRecuperarPuntajeClasificacionYVersion() throws Exception {
        mockMvc.perform(get(RUTA_EVALUACIONES + "/" + idEvaluacionConPolitica1).with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvaluacion").value(idEvaluacionConPolitica1))
                .andExpect(jsonPath("$.scoreTotal").value(50))
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.versionPolitica").value(1))
                .andExpect(jsonPath("$.detalles[0].puntajeObtenido").value(50));
    }

    @Test
    @Order(4)
    void unCambioDePoliticaNoDebeReclasificarLasEvaluacionesExistentes() throws Exception {
        publicar(POLITICA_2);

        mockMvc.perform(get(RUTA_EVALUACIONES + "/" + idEvaluacionConPolitica1).with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.versionPolitica").value(1));

        mockMvc.perform(calcular().with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("BAJO"))
                .andExpect(jsonPath("$.versionPolitica").value(2));
    }

    @Test
    @Order(5)
    void unaEvaluacionInexistenteDebeResponder404() throws Exception {
        mockMvc.perform(get(RUTA_EVALUACIONES + "/999999999").with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_ENCONTRADA"));
    }

    @Test
    @Order(6)
    void soloElAnalistaDebeConsultarEvaluacionesPorId() throws Exception {
        mockMvc.perform(get(RUTA_EVALUACIONES + "/" + idEvaluacionConPolitica1)
                        .with(conRol(JwtConfig.ROL_ADMINISTRADOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
        mockMvc.perform(get(RUTA_EVALUACIONES + "/" + idEvaluacionConPolitica1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void unPuntajeSinClasificacionDebeResponder409SinPersistirLaEvaluacion() throws Exception {
        // segunda regla: 50 + 100 = 150, fuera de todos los rangos (-100 a 130)
        long idRiesgoMoras = numero("INSERT INTO riesgo (variable, descripcion) VALUES ('NUMERO_MORAS', "
                + "'Número de moras del solicitante') RETURNING id_riesgo");
        numero("INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) VALUES (" + idRiesgoMoras
                + ", '=', '0', 100) RETURNING id_regla");
        long evaluaciones = numero("SELECT count(*) FROM evaluacion");
        long detalles = numero("SELECT count(*) FROM detalle_evaluacion");

        mockMvc.perform(calcular().with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PUNTAJE_SIN_CLASIFICACION"));

        assertThat(numero("SELECT count(*) FROM evaluacion")).isEqualTo(evaluaciones);
        assertThat(numero("SELECT count(*) FROM detalle_evaluacion")).isEqualTo(detalles);
    }

    private MockHttpServletRequestBuilder calcular() {
        return post(RUTA_EVALUACIONES).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idSolicitante\": " + idSolicitante + "}");
    }

    private void publicar(String politica) throws Exception {
        mockMvc.perform(post(RUTA_POLITICAS).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(politica))
                .andExpect(status().isCreated());
    }

    private long numero(String sql) throws SQLException {
        return Long.parseLong(texto(sql));
    }

    private String texto(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
