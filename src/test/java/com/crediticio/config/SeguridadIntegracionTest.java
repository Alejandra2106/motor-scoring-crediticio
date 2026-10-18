package com.crediticio.config;

import com.crediticio.applicants.ports.input.ConsultarSolicitanteUseCase;
import com.crediticio.applicants.ports.input.RegistrarSolicitanteUseCase;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.input.ConsultarEvaluacionUseCase;
import com.crediticio.riskvariables.ports.input.CambiarEstadoVariableRiesgoUseCase;
import com.crediticio.riskvariables.ports.input.CrearVariableRiesgoUseCase;
import com.crediticio.scoring.ports.input.ConsultarPoliticaVigenteUseCase;
import com.crediticio.scoring.ports.input.CrearReglaScoringUseCase;
import com.crediticio.scoring.ports.input.EditarReglaScoringUseCase;
import com.crediticio.scoring.ports.input.PublicarPoliticaClasificacionUseCase;
import com.crediticio.support.ClavesRsaPrueba;
import com.crediticio.support.TokensPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU10 de punta a punta en scoring-api: cadena de filtros real, tokens RS256 firmados con una clave
 * efímera y validados con su clave pública, y la matriz de permisos por endpoint (CA01–CA08).
 * Los casos de uso están simulados: aquí solo se verifica la autenticación y la autorización.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadIntegracionTest {

    private static final String EMISOR = "auth-api-prueba";
    private static final String ADMIN = JwtConfig.ROL_ADMINISTRADOR;
    private static final String ANALISTA = JwtConfig.ROL_ANALISTA;
    private static final KeyPair CLAVES = ClavesRsaPrueba.generar();
    private static final KeyPair OTRAS_CLAVES = ClavesRsaPrueba.generar();

    @TempDir
    static Path directorioClaves;

    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("scoring.jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(CLAVES, directorioClaves));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarSolicitanteUseCase registrarSolicitanteUseCase;
    @MockitoBean
    private ConsultarSolicitanteUseCase consultarSolicitanteUseCase;
    @MockitoBean
    private CalcularScoreUseCase calcularScoreUseCase;
    @MockitoBean
    private ConsultarEvaluacionUseCase consultarEvaluacionUseCase;
    @MockitoBean
    private CrearVariableRiesgoUseCase crearVariableRiesgoUseCase;
    @MockitoBean
    private CambiarEstadoVariableRiesgoUseCase cambiarEstadoVariableRiesgoUseCase;
    @MockitoBean
    private CrearReglaScoringUseCase crearReglaScoringUseCase;
    @MockitoBean
    private EditarReglaScoringUseCase editarReglaScoringUseCase;
    @MockitoBean
    private ConsultarPoliticaVigenteUseCase consultarPoliticaVigenteUseCase;
    @MockitoBean
    private PublicarPoliticaClasificacionUseCase publicarPoliticaClasificacionUseCase;

    /** Matriz aprobada para HU10 (docs/security/matriz-permisos-scoring-api.md). */
    record Endpoint(HttpMethod metodo, String ruta, Set<String> rolesPermitidos) {

        MockHttpServletRequestBuilder peticion() {
            MockHttpServletRequestBuilder builder = request(metodo, ruta);
            return metodo == HttpMethod.GET ? builder : builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }

        @Override
        public String toString() {
            return metodo + " " + ruta;
        }
    }

    static Stream<Endpoint> endpoints() {
        return Stream.of(
                new Endpoint(HttpMethod.POST, "/api/v1/solicitantes", Set.of(ANALISTA)),
                new Endpoint(HttpMethod.GET, "/api/v1/solicitantes/documento/1234567", Set.of(ADMIN, ANALISTA)),
                new Endpoint(HttpMethod.POST, "/api/v1/evaluaciones", Set.of(ANALISTA)),
                new Endpoint(HttpMethod.GET, "/api/v1/evaluaciones/1", Set.of(ANALISTA)),
                new Endpoint(HttpMethod.POST, "/api/v1/variables-riesgo", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/variables-riesgo/25/estado", Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/reglas-scoring", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/reglas-scoring/7", Set.of(ADMIN)),
                new Endpoint(HttpMethod.GET, "/api/v1/politicas-clasificacion/vigente", Set.of(ADMIN, ANALISTA)),
                new Endpoint(HttpMethod.POST, "/api/v1/politicas-clasificacion", Set.of(ADMIN)));
    }

    static Stream<Endpoint> endpointsConRolNoPermitido() {
        return endpoints().filter(endpoint -> endpoint.rolesPermitidos().size() < 2);
    }

    private static String bearer(String rol) {
        return "Bearer " + TokensPrueba.firmar(CLAVES, TokensPrueba.claimsValidos(EMISOR, rol).build());
    }

    private static String rolNoPermitido(Endpoint endpoint) {
        return endpoint.rolesPermitidos().contains(ADMIN) ? ANALISTA : ADMIN;
    }

    private static void traceIdDelCuerpoIgualAlDeLaCabecera(MvcResult resultado) throws Exception {
        String cabecera = resultado.getResponse().getHeader("X-Trace-Id");
        assertThat(cabecera).isNotBlank();
        assertThat(resultado.getResponse().getContentAsString()).contains("\"traceId\":\"" + cabecera + "\"");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void sinTokenDebeResponder401ConApiErrorYTraceId(Endpoint endpoint) throws Exception {
        MvcResult resultado = mockMvc.perform(endpoint.peticion())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"))
                .andReturn();

        traceIdDelCuerpoIgualAlDeLaCabecera(resultado);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void conUnTokenMalFormadoDebeResponder401(Endpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.peticion().header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void conUnTokenExpiradoDebeResponder401(Endpoint endpoint) throws Exception {
        String rol = endpoint.rolesPermitidos().iterator().next();
        String expirado = TokensPrueba.firmar(CLAVES, TokensPrueba.claimsValidos(EMISOR, rol)
                .issueTime(Date.from(Instant.now().minus(Duration.ofMinutes(20))))
                .expirationTime(Date.from(Instant.now().minus(Duration.ofMinutes(5))))
                .build());

        mockMvc.perform(endpoint.peticion().header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void conUnTokenFirmadoPorOtraClaveDebeResponder401(Endpoint endpoint) throws Exception {
        String rol = endpoint.rolesPermitidos().iterator().next();
        String ajeno = TokensPrueba.firmar(OTRAS_CLAVES, TokensPrueba.claimsValidos(EMISOR, rol).build());

        mockMvc.perform(endpoint.peticion().header(HttpHeaders.AUTHORIZATION, "Bearer " + ajeno))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void conUnRolPermitidoDebeSuperarLaAutorizacion(Endpoint endpoint) throws Exception {
        for (String rol : endpoint.rolesPermitidos()) {
            int estado = mockMvc.perform(endpoint.peticion().header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andReturn().getResponse().getStatus();

            assertThat(estado).as("%s con rol %s", endpoint, rol).isNotIn(401, 403);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointsConRolNoPermitido")
    void conUnRolNoPermitidoDebeResponder403ConApiErrorYTraceId(Endpoint endpoint) throws Exception {
        MvcResult resultado = mockMvc.perform(endpoint.peticion()
                        .header(HttpHeaders.AUTHORIZATION, bearer(rolNoPermitido(endpoint))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"))
                .andReturn();

        traceIdDelCuerpoIgualAlDeLaCabecera(resultado);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void conUnRolDesconocidoDebeResponder403(Endpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.peticion().header(HttpHeaders.AUTHORIZATION, bearer("AUDITOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
    }

    @Test
    void elRolEnviadoEnElCuerpoOEnCabecerasNoDebeOtorgarPermisos() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/v1/variables-riesgo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALISTA))
                        .header("X-Role", ADMIN)
                        .header("role", ADMIN)
                        .param("role", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + ADMIN + "\",\"rol\":\"" + ADMIN + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaRutaNoMapeadaDebeQuedarProtegida() throws Exception {
        mockMvc.perform(get("/api/v1/ruta-inexistente"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/ruta-inexistente").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthDebeSerPublicoYSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void otrosEndpointsDeActuatorNoDebenSerAccesibles() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerDebeQuedarProtegidoFueraDelPerfilDev() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
    }

    @Test
    void noDebeCrearseSesionHttp() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/solicitantes/documento/1234567")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALISTA)))
                .andReturn();

        assertThat(resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    void elPreflightDesdeUnOrigenPermitidoDebeAdmitirAuthorization() throws Exception {
        mockMvc.perform(options("/api/v1/solicitantes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                        .containsIgnoringCase("Authorization"));
    }

    @Test
    void elPreflightDesdeUnOrigenNoPermitidoDebeRechazarse() throws Exception {
        mockMvc.perform(options("/api/v1/solicitantes")
                        .header(HttpHeaders.ORIGIN, "https://sitio-no-permitido.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasRespuestasDebenExponerXTraceIdAlNavegador() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/documento/1234567")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ANALISTA)))
                .andExpect(result -> assertThat(List.of(
                        result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).split(",\\s*")))
                        .contains("X-Trace-Id"));
    }
}
