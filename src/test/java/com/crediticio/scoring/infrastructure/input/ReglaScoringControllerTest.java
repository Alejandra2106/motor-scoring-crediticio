package com.crediticio.scoring.infrastructure.input;

import com.crediticio.scoring.application.dto.CrearReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.domain.ReglaScoringVariableInactivaException;
import com.crediticio.scoring.domain.ReglaScoringVariableNoEncontradaException;
import com.crediticio.scoring.ports.input.CrearReglaScoringUseCase;
import com.crediticio.shared.exception.GlobalExceptionHandler;
import com.crediticio.shared.util.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReglaScoringController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class ReglaScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CrearReglaScoringUseCase crearReglaScoringUseCase;

    @Test
    void debeRetornar201YExactamenteLosCincoCamposAprobadosCuandoLaCreacionEsExitosa() throws Exception {
        CrearReglaScoringRequest request = solicitudValida();
        ReglaScoringResponse response = new ReglaScoringResponse(1L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20);
        when(crearReglaScoringUseCase.crear(any(CrearReglaScoringRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.idRegla").value(1))
                .andExpect(jsonPath("$.idRiesgo").value(1))
                .andExpect(jsonPath("$.operador").value(">="))
                .andExpect(jsonPath("$.valorCondicion").value("3000000"))
                .andExpect(jsonPath("$.puntaje").value(20));
    }

    @Test
    void debeRetornar400CuandoFaltaElIdRiesgo() throws Exception {
        String payloadSinIdRiesgo = """
                {
                  "operador": ">=",
                  "valorCondicion": "3000000",
                  "puntaje": 20
                }
                """;

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadSinIdRiesgo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar400CuandoElOperadorNoPerteneceAlConjuntoPermitido() throws Exception {
        String payloadConOperadorInvalido = """
                {
                  "idRiesgo": 1,
                  "operador": "!=",
                  "valorCondicion": "3000000",
                  "puntaje": 20
                }
                """;

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConOperadorInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar400CuandoElPuntajeExcedeElRangoPermitido() throws Exception {
        String payloadConPuntajeFueraDeRango = """
                {
                  "idRiesgo": 1,
                  "operador": ">=",
                  "valorCondicion": "3000000",
                  "puntaje": 101
                }
                """;

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConPuntajeFueraDeRango))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debeRetornar400YNoLlegarAlCasoDeUsoCuandoElPuntajeEsElDecimalReportado() throws Exception {
        // Reproduce exactamente el defecto reportado: Jackson truncaba "puntaje": 20.5 a 20
        // en vez de rechazarlo (RF09 exige un entero estricto, sin truncar ni redondear).
        String payloadConPuntajeDecimalReportado = """
                {
                  "idRiesgo": 109,
                  "operador": ">=",
                  "valorCondicion": "600000",
                  "puntaje": 20.5
                }
                """;

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConPuntajeDecimalReportado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(crearReglaScoringUseCase, never()).crear(any(CrearReglaScoringRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"20.0", "-20.5"})
    void debeRetornar400YNoLlegarAlCasoDeUsoCuandoElPuntajeTieneNotacionDecimal(String puntajeConDecimal) throws Exception {
        String payload = """
                {
                  "idRiesgo": 1,
                  "operador": ">=",
                  "valorCondicion": "3000000",
                  "puntaje": %s
                }
                """.formatted(puntajeConDecimal);

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(crearReglaScoringUseCase, never()).crear(any(CrearReglaScoringRequest.class));
    }

    @Test
    void debeRetornar400CuandoElPuntajeNoEsNumerico() throws Exception {
        String payloadConPuntajeNoNumerico = """
                {
                  "idRiesgo": 1,
                  "operador": ">=",
                  "valorCondicion": "3000000",
                  "puntaje": "abc"
                }
                """;

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConPuntajeNoNumerico))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(crearReglaScoringUseCase, never()).crear(any(CrearReglaScoringRequest.class));
    }

    @Test
    void debeRetornar400CuandoElPuntajeEsMenorAMenos100() throws Exception {
        String payloadConPuntajeFueraDeRango = """
                {
                  "idRiesgo": 1,
                  "operador": ">=",
                  "valorCondicion": "3000000",
                  "puntaje": -101
                }
                """;

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConPuntajeFueraDeRango))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-100, 100})
    void debeAceptarPuntajeEnteroEnLosLimitesDelRango(int puntajeValido) throws Exception {
        ReglaScoringResponse response = new ReglaScoringResponse(1L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000",
                puntajeValido);
        when(crearReglaScoringUseCase.crear(any(CrearReglaScoringRequest.class))).thenReturn(response);

        String payload = """
                {
                  "idRiesgo": 1,
                  "operador": ">=",
                  "valorCondicion": "3000000",
                  "puntaje": %d
                }
                """.formatted(puntajeValido);

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(puntajeValido));
    }

    @Test
    void debeRetornar404CuandoLaVariableDeRiesgoNoExiste() throws Exception {
        when(crearReglaScoringUseCase.crear(any(CrearReglaScoringRequest.class)))
                .thenThrow(new ReglaScoringVariableNoEncontradaException());

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("VARIABLE_RIESGO_NO_ENCONTRADA"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar409CuandoLaVariableDeRiesgoEstaInactiva() throws Exception {
        when(crearReglaScoringUseCase.crear(any(CrearReglaScoringRequest.class)))
                .thenThrow(new ReglaScoringVariableInactivaException());

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VARIABLE_RIESGO_INACTIVA"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar409CuandoLaReglaEstaDuplicada() throws Exception {
        when(crearReglaScoringUseCase.crear(any(CrearReglaScoringRequest.class)))
                .thenThrow(new ReglaScoringDuplicadaException());

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REGLA_SCORING_DUPLICADA"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void debeRetornar500SinExponerDetallesInternosAnteUnErrorNoControlado() throws Exception {
        when(crearReglaScoringUseCase.crear(any(CrearReglaScoringRequest.class)))
                .thenThrow(new RuntimeException("fallo inesperado de infraestructura"));

        mockMvc.perform(post("/api/v1/reglas-scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    private CrearReglaScoringRequest solicitudValida() {
        return new CrearReglaScoringRequest(1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20);
    }
}
