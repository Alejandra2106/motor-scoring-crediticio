package com.crediticio.applicants.infrastructure.input;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteResponse;
import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.applicants.domain.HistorialCrediticio;
import com.crediticio.applicants.ports.input.RegistrarSolicitanteUseCase;
import com.crediticio.shared.exception.GlobalExceptionHandler;
import com.crediticio.shared.util.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitanteController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class SolicitanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RegistrarSolicitanteUseCase registrarSolicitanteUseCase;

    @Test
    void debeRetornar201YSoloLosCuatroCamposAprobadosCuandoElRegistroEsExitoso() throws Exception {
        RegistrarSolicitanteRequest request = solicitudValida();
        SolicitanteResponse response = new SolicitanteResponse(
                1L, "Ana María Pérez", "1234567", LocalDateTime.of(2026, 1, 1, 10, 0));
        when(registrarSolicitanteUseCase.registrar(any(RegistrarSolicitanteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/solicitantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.idSolicitante").value(1))
                .andExpect(jsonPath("$.nombreCompleto").value("Ana María Pérez"))
                .andExpect(jsonPath("$.numeroDocumento").value("1234567"))
                .andExpect(jsonPath("$.fechaRegistro").exists())
                .andExpect(jsonPath("$.ingresosMensuales").doesNotExist())
                .andExpect(jsonPath("$.deudasMensuales").doesNotExist())
                .andExpect(jsonPath("$.numeroMoras").doesNotExist())
                .andExpect(jsonPath("$.historialCrediticio").doesNotExist())
                .andExpect(jsonPath("$.antiguedadLaboral").doesNotExist());

        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void debeRetornar400ConDetallesCuandoLosCamposSonInvalidos() throws Exception {
        String payloadInvalido = """
                {
                  "nombreCompleto": "An",
                  "numeroDocumento": "123",
                  "ingresosMensuales": -1,
                  "deudasMensuales": -1,
                  "numeroMoras": -1,
                  "historialCrediticio": "BUENO",
                  "antiguedadLaboral": -1
                }
                """;

        mockMvc.perform(post("/api/v1/solicitantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar400CuandoElHistorialCrediticioNoEsReconocido() throws Exception {
        String payloadConHistorialInvalido = """
                {
                  "nombreCompleto": "Ana María Pérez",
                  "numeroDocumento": "1234567",
                  "ingresosMensuales": 3000000,
                  "deudasMensuales": 500000,
                  "numeroMoras": 0,
                  "historialCrediticio": "EXCELENTE",
                  "antiguedadLaboral": 2.5
                }
                """;

        mockMvc.perform(post("/api/v1/solicitantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConHistorialInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar409CuandoElDocumentoYaExiste() throws Exception {
        when(registrarSolicitanteUseCase.registrar(any(RegistrarSolicitanteRequest.class)))
                .thenThrow(new DocumentoDuplicadoException());

        mockMvc.perform(post("/api/v1/solicitantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENTO_DUPLICADO"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar500SinExponerDetallesInternosAnteUnErrorNoControlado() throws Exception {
        when(registrarSolicitanteUseCase.registrar(any(RegistrarSolicitanteRequest.class)))
                .thenThrow(new RuntimeException("fallo inesperado de infraestructura"));

        mockMvc.perform(post("/api/v1/solicitantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intente nuevamente más tarde"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeReutilizarUnXTraceIdValidoRecibido() throws Exception {
        SolicitanteResponse response = new SolicitanteResponse(
                1L, "Ana María Pérez", "1234567", LocalDateTime.of(2026, 1, 1, 10, 0));
        when(registrarSolicitanteUseCase.registrar(any(RegistrarSolicitanteRequest.class))).thenReturn(response);
        String traceIdEnviado = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

        mockMvc.perform(post("/api/v1/solicitantes")
                        .header("X-Trace-Id", traceIdEnviado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", traceIdEnviado));
    }

    @Test
    void debeGenerarUnNuevoXTraceIdCuandoElRecibidoNoEsValido() throws Exception {
        SolicitanteResponse response = new SolicitanteResponse(
                1L, "Ana María Pérez", "1234567", LocalDateTime.of(2026, 1, 1, 10, 0));
        when(registrarSolicitanteUseCase.registrar(any(RegistrarSolicitanteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/solicitantes")
                        .header("X-Trace-Id", "no-es-un-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isCreated())
                .andExpect(result -> {
                    String traceIdRespuesta = result.getResponse().getHeader("X-Trace-Id");
                    assertThat(traceIdRespuesta).isNotEqualTo("no-es-un-uuid");
                    assertThat(traceIdRespuesta).matches(
                            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
                });
    }

    private RegistrarSolicitanteRequest solicitudValida() {
        return new RegistrarSolicitanteRequest(
                "Ana María Pérez",
                "1234567",
                new BigDecimal("3000000"),
                new BigDecimal("500000"),
                0,
                HistorialCrediticio.BUENO,
                new BigDecimal("2.5"));
    }
}
