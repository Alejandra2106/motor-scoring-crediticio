package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.DetalleEvaluacionResponse;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.EvaluacionNoEncontradaException;
import com.crediticio.evaluations.domain.EvaluacionSinPoliticaVigenteException;
import com.crediticio.evaluations.domain.NivelEndeudamientoIndeterminadoException;
import com.crediticio.evaluations.domain.PuntajeSinClasificacionException;
import com.crediticio.evaluations.domain.SolicitanteEvaluacionNoEncontradoException;
import com.crediticio.config.JwtConfig;
import com.crediticio.config.SecurityConfig;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.input.ConsultarEvaluacionUseCase;
import com.crediticio.shared.exception.GlobalExceptionHandler;
import com.crediticio.shared.util.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluacionController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class, SecurityConfig.class})
class EvaluacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** HU10: la cadena de seguridad real exige un JWT; aquí se simula el token ya validado con su rol. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor conRol(String rol) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim(JwtConfig.CLAIM_ROL, rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CalcularScoreUseCase calcularScoreUseCase;

    @MockitoBean
    private ConsultarEvaluacionUseCase consultarEvaluacionUseCase;

    private static EvaluacionResponse evaluacionClasificada() {
        return new EvaluacionResponse(1L, 1L, LocalDateTime.of(2026, 1, 1, 10, 0), 20, "MEDIO", 3L, 2,
                List.of(new DetalleEvaluacionResponse(10L, ">=", "3000000", true, 20)));
    }

    @Test
    void debeRetornar201ConElResultadoDelCalculoCuandoEsExitoso() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class))).thenReturn(evaluacionClasificada());

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.idEvaluacion").value(1))
                .andExpect(jsonPath("$.idSolicitante").value(1))
                .andExpect(jsonPath("$.scoreTotal").value(20))
                .andExpect(jsonPath("$.detalles[0].idRegla").value(10))
                .andExpect(jsonPath("$.detalles[0].operadorAplicado").value(">="))
                .andExpect(jsonPath("$.detalles[0].condicionCumplida").value(true))
                .andExpect(jsonPath("$.detalles[0].puntajeObtenido").value(20));
    }

    @Test
    void la201DebeIncluirLaCategoriaYLaVersionDeLaPoliticaSinInterpretarlaComoDecisionDeCredito() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class))).thenReturn(evaluacionClasificada());

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.idPolitica").value(3))
                .andExpect(jsonPath("$.versionPolitica").value(2))
                .andExpect(jsonPath("$.aprobado").doesNotExist())
                .andExpect(jsonPath("$.decision").doesNotExist());
    }

    @Test
    void debeRetornar409SinPoliticaVigente() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class)))
                .thenThrow(new EvaluacionSinPoliticaVigenteException());

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POLITICA_VIGENTE_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void debeRetornar409CuandoElPuntajeNoTieneClasificacion() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class)))
                .thenThrow(new PuntajeSinClasificacionException(131));

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PUNTAJE_SIN_CLASIFICACION"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar200ConPuntajeClasificacionYVersionAlConsultarPorId() throws Exception {
        when(consultarEvaluacionUseCase.consultarPorId(1L)).thenReturn(evaluacionClasificada());

        mockMvc.perform(get("/api/v1/evaluaciones/1").with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvaluacion").value(1))
                .andExpect(jsonPath("$.scoreTotal").value(20))
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.idPolitica").value(3))
                .andExpect(jsonPath("$.versionPolitica").value(2))
                .andExpect(jsonPath("$.detalles[0].puntajeObtenido").value(20));
    }

    @Test
    void debeRetornar404AlConsultarUnaEvaluacionInexistente() throws Exception {
        when(consultarEvaluacionUseCase.consultarPorId(99L)).thenThrow(new EvaluacionNoEncontradaException());

        mockMvc.perform(get("/api/v1/evaluaciones/99").with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_ENCONTRADA"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void debeRetornar400AlConsultarConUnIdentificadorNoNumerico() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/abc").with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(consultarEvaluacionUseCase);
    }

    @Test
    void elAdministradorNoDebeConsultarEvaluacionesPorId() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/1").with(conRol(JwtConfig.ROL_ADMINISTRADOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));

        verifyNoInteractions(consultarEvaluacionUseCase);
    }

    @Test
    void debeRetornar400YNoLlegarAlCasoDeUsoCuandoElIdSolicitanteEsDecimal() throws Exception {
        String payloadConIdSolicitanteDecimal = """
                {
                  "idSolicitante": 3.7
                }
                """;

        MvcResult resultado = mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConIdSolicitanteDecimal))
                .andReturn();

        List<Long> idSolicitanteRecibido = mockingDetails(calcularScoreUseCase).getInvocations().stream()
                .map(invocacion -> ((CalcularScoreRequest) invocacion.getArgument(0)).getIdSolicitante())
                .toList();
        assertThat(resultado.getResponse().getStatus())
                .as("estado HTTP; idSolicitante recibido por el caso de uso: %s", idSolicitanteRecibido)
                .isEqualTo(400);
        assertThat(idSolicitanteRecibido).as("idSolicitante recibido por el caso de uso").isEmpty();
    }

    @Test
    void debeRetornar400CuandoSeEnvianAmbosIdentificadores() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, "1234567890"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar400CuandoNoSeEnviaNingunIdentificador() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar400CuandoElNumeroDocumentoTieneFormatoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(null, "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar404CuandoElSolicitanteNoExiste() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class)))
                .thenThrow(new SolicitanteEvaluacionNoEncontradoException());

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(99L, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SOLICITANTE_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar409CuandoNivelEndeudamientoEsIndeterminado() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class)))
                .thenThrow(new NivelEndeudamientoIndeterminadoException());

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NIVEL_ENDEUDAMIENTO_INDETERMINADO"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar500SinExponerDetallesInternosAnteUnErrorNoControlado() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class)))
                .thenThrow(new RuntimeException("fallo inesperado de infraestructura"));

        mockMvc.perform(post("/api/v1/evaluaciones").with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
