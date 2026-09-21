package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.DetalleEvaluacionResponse;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.NivelEndeudamientoIndeterminadoException;
import com.crediticio.evaluations.domain.SolicitanteEvaluacionNoEncontradoException;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.shared.exception.GlobalExceptionHandler;
import com.crediticio.shared.util.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluacionController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class EvaluacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CalcularScoreUseCase calcularScoreUseCase;

    @Test
    void debeRetornar201ConElResultadoDelCalculoCuandoEsExitoso() throws Exception {
        EvaluacionResponse response = new EvaluacionResponse(1L, 1L, LocalDateTime.of(2026, 1, 1, 10, 0), 20,
                List.of(new DetalleEvaluacionResponse(10L, ">=", "3000000", true, 20)));
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/evaluaciones")
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
    void debeRetornar400CuandoSeEnvianAmbosIdentificadores() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, "1234567890"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar400CuandoNoSeEnviaNingunIdentificador() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar400CuandoElNumeroDocumentoTieneFormatoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(null, "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar404CuandoElSolicitanteNoExiste() throws Exception {
        when(calcularScoreUseCase.calcular(any(CalcularScoreRequest.class)))
                .thenThrow(new SolicitanteEvaluacionNoEncontradoException());

        mockMvc.perform(post("/api/v1/evaluaciones")
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

        mockMvc.perform(post("/api/v1/evaluaciones")
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

        mockMvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularScoreRequest(1L, null))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
