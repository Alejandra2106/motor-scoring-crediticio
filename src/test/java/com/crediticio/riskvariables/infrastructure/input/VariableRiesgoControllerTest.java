package com.crediticio.riskvariables.infrastructure.input;

import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoResponse;
import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;
import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.TipoVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import com.crediticio.riskvariables.domain.VariableRiesgoNoEncontradaException;
import com.crediticio.riskvariables.ports.input.CambiarEstadoVariableRiesgoUseCase;
import com.crediticio.riskvariables.ports.input.CrearVariableRiesgoUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VariableRiesgoController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class VariableRiesgoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CrearVariableRiesgoUseCase crearVariableRiesgoUseCase;

    @MockitoBean
    private CambiarEstadoVariableRiesgoUseCase cambiarEstadoVariableRiesgoUseCase;

    @Test
    void debeRetornar201YExactamenteLosCincoCamposAprobadosCuandoLaCreacionEsExitosa() throws Exception {
        CrearVariableRiesgoRequest request = solicitudValida();
        VariableRiesgoResponse response = new VariableRiesgoResponse(
                1L, NombreVariableRiesgo.INGRESOS_MENSUALES, TipoVariableRiesgo.NUMERICO,
                "Ingresos mensuales netos declarados por el solicitante", EstadoVariableRiesgo.ACTIVA);
        when(crearVariableRiesgoUseCase.crear(any(CrearVariableRiesgoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/variables-riesgo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.idVariableRiesgo").value(1))
                .andExpect(jsonPath("$.variable").value("INGRESOS_MENSUALES"))
                .andExpect(jsonPath("$.tipo").value("NUMERICO"))
                .andExpect(jsonPath("$.descripcion").value("Ingresos mensuales netos declarados por el solicitante"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.fechaCreacion").doesNotExist())
                .andExpect(jsonPath("$.usuarioCreador").doesNotExist());
    }

    @Test
    void debeRetornar400ConDetallesCuandoLaDescripcionEsInvalida() throws Exception {
        String payloadInvalido = """
                {
                  "variable": "INGRESOS_MENSUALES",
                  "descripcion": "corta"
                }
                """;

        mockMvc.perform(post("/api/v1/variables-riesgo")
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
    void debeRetornar400CuandoLaVariableNoPerteneceAlConjuntoPermitido() throws Exception {
        String payloadConVariableInvalida = """
                {
                  "variable": "PATRIMONIO_NETO",
                  "descripcion": "Descripción válida con más de diez caracteres"
                }
                """;

        mockMvc.perform(post("/api/v1/variables-riesgo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConVariableInvalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar400CuandoFaltaLaVariable() throws Exception {
        String payloadSinVariable = """
                {
                  "descripcion": "Descripción válida con más de diez caracteres"
                }
                """;

        mockMvc.perform(post("/api/v1/variables-riesgo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadSinVariable))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar409CuandoLaVariableYaExiste() throws Exception {
        when(crearVariableRiesgoUseCase.crear(any(CrearVariableRiesgoRequest.class)))
                .thenThrow(new VariableRiesgoDuplicadaException());

        mockMvc.perform(post("/api/v1/variables-riesgo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VARIABLE_RIESGO_DUPLICADA"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar500SinExponerDetallesInternosAnteUnErrorNoControlado() throws Exception {
        when(crearVariableRiesgoUseCase.crear(any(CrearVariableRiesgoRequest.class)))
                .thenThrow(new RuntimeException("fallo inesperado de infraestructura"));

        mockMvc.perform(post("/api/v1/variables-riesgo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intente nuevamente más tarde"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    private CrearVariableRiesgoRequest solicitudValida() {
        return new CrearVariableRiesgoRequest(
                NombreVariableRiesgo.INGRESOS_MENSUALES,
                "Ingresos mensuales netos declarados por el solicitante");
    }

    @Test
    void debeRetornar200ConIdRiesgoEstadoAnteriorYEstadoNuevoCuandoElCambioEsExitoso() throws Exception {
        CambiarEstadoVariableRiesgoResponse response = new CambiarEstadoVariableRiesgoResponse(
                25L, EstadoVariableRiesgo.ACTIVA, EstadoVariableRiesgo.INACTIVA);
        when(cambiarEstadoVariableRiesgoUseCase.cambiarEstado(eq(25L), any(CambiarEstadoVariableRiesgoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/variables-riesgo/25/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo.INACTIVA))))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.idRiesgo").value(25))
                .andExpect(jsonPath("$.estadoAnterior").value("ACTIVA"))
                .andExpect(jsonPath("$.estadoNuevo").value("INACTIVA"));
    }

    @Test
    void debeRetornar404CuandoLaVariableNoExiste() throws Exception {
        when(cambiarEstadoVariableRiesgoUseCase.cambiarEstado(eq(999L), any(CambiarEstadoVariableRiesgoRequest.class)))
                .thenThrow(new VariableRiesgoNoEncontradaException());

        mockMvc.perform(patch("/api/v1/variables-riesgo/999/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo.INACTIVA))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("VARIABLE_RIESGO_NO_ENCONTRADA"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar400CuandoElEstadoNoPerteneceAlConjuntoPermitido() throws Exception {
        String payloadConEstadoInvalido = """
                { "estado": "SUSPENDIDA" }
                """;

        mockMvc.perform(patch("/api/v1/variables-riesgo/25/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConEstadoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar400CuandoElEstadoEsNuloOAusente() throws Exception {
        String payloadSinEstado = "{}";

        mockMvc.perform(patch("/api/v1/variables-riesgo/25/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadSinEstado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar400CuandoElIdRiesgoNoEsNumerico() throws Exception {
        mockMvc.perform(patch("/api/v1/variables-riesgo/abc/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo.INACTIVA))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
