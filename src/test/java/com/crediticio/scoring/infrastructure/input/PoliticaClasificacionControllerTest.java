package com.crediticio.scoring.infrastructure.input;

import com.crediticio.config.JwtConfig;
import com.crediticio.config.SecurityConfig;
import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;
import com.crediticio.scoring.application.dto.RangoClasificacionResponse;
import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.scoring.domain.PoliticaClasificacionInvalidaException;
import com.crediticio.scoring.domain.PoliticaVigenteNoDisponibleException;
import com.crediticio.scoring.domain.PublicacionPoliticaConcurrenteException;
import com.crediticio.scoring.domain.PublicacionSinAutorException;
import com.crediticio.scoring.ports.input.ConsultarPoliticaVigenteUseCase;
import com.crediticio.scoring.ports.input.PublicarPoliticaClasificacionUseCase;
import com.crediticio.shared.exception.GlobalExceptionHandler;
import com.crediticio.shared.util.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU11 CA01, CA02, CA09, CA10 y CA16 a través de HTTP, con la cadena de seguridad de HU10. */
@WebMvcTest(PoliticaClasificacionController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class, SecurityConfig.class})
class PoliticaClasificacionControllerTest {

    private static final String RUTA_VIGENTE = "/api/v1/politicas-clasificacion/vigente";
    private static final String RUTA_PUBLICAR = "/api/v1/politicas-clasificacion";
    private static final String POLITICA_VALIDA = """
            {"rangos": [
              {"categoria": "ALTO", "limiteInferior": -100, "limiteSuperior": 29},
              {"categoria": "MEDIO", "limiteInferior": 30, "limiteSuperior": 79},
              {"categoria": "BAJO", "limiteInferior": 80, "limiteSuperior": 130}
            ]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ConsultarPoliticaVigenteUseCase consultarPoliticaVigenteUseCase;

    @MockitoBean
    private PublicarPoliticaClasificacionUseCase publicarPoliticaClasificacionUseCase;

    private static RequestPostProcessor conRol(String rol) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim(JwtConfig.CLAIM_ROL, rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static PoliticaClasificacionResponse politica(int version) {
        LocalDateTime publicada = LocalDateTime.of(2026, 10, 17, 9, 30);
        return new PoliticaClasificacionResponse(10L + version, version, publicada, publicada, List.of(
                new RangoClasificacionResponse(1L, CategoriaRiesgo.ALTO, -100, 29),
                new RangoClasificacionResponse(2L, CategoriaRiesgo.MEDIO, 30, 79),
                new RangoClasificacionResponse(3L, CategoriaRiesgo.BAJO, 80, 130)));
    }

    // ---------- GET /vigente (CA01): ambos roles ----------

    @ParameterizedTest
    @ValueSource(strings = {JwtConfig.ROL_ADMINISTRADOR, JwtConfig.ROL_ANALISTA})
    void ambosRolesDebenConsultarLaPoliticaVigente(String rol) throws Exception {
        when(consultarPoliticaVigenteUseCase.consultarVigente()).thenReturn(politica(3));

        mockMvc.perform(get(RUTA_VIGENTE).with(conRol(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPolitica").value(13))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.fechaPublicacion").exists())
                .andExpect(jsonPath("$.rangos.length()").value(3))
                .andExpect(jsonPath("$.rangos[0].categoria").value("ALTO"))
                .andExpect(jsonPath("$.rangos[0].limiteInferior").value(-100))
                .andExpect(jsonPath("$.rangos[2].limiteSuperior").value(130))
                .andExpect(jsonPath("$.publicadoPor").doesNotExist());
    }

    @Test
    void sinPoliticaVigenteDebeResponder404() throws Exception {
        when(consultarPoliticaVigenteUseCase.consultarVigente()).thenThrow(new PoliticaVigenteNoDisponibleException());

        mockMvc.perform(get(RUTA_VIGENTE).with(conRol(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POLITICA_VIGENTE_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void consultarSinTokenDebeResponder401() throws Exception {
        mockMvc.perform(get(RUTA_VIGENTE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));

        verifyNoInteractions(consultarPoliticaVigenteUseCase);
    }

    @Test
    void consultarConUnRolDesconocidoDebeResponder403() throws Exception {
        mockMvc.perform(get(RUTA_VIGENTE).with(conRol("AUDITOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));

        verifyNoInteractions(consultarPoliticaVigenteUseCase);
    }

    // ---------- POST (CA02, CA16): solo ADMINISTRADOR_RIESGO ----------

    @Test
    void unAdministradorDebePublicarUnaNuevaVersionYRecibir201() throws Exception {
        when(publicarPoliticaClasificacionUseCase.publicar(any())).thenReturn(politica(4));

        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(POLITICA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.rangos.length()").value(3))
                .andExpect(jsonPath("$.publicadoPor").doesNotExist());

        ArgumentCaptor<PublicarPoliticaClasificacionRequest> solicitud =
                ArgumentCaptor.forClass(PublicarPoliticaClasificacionRequest.class);
        verify(publicarPoliticaClasificacionUseCase).publicar(solicitud.capture());
        assertThat(solicitud.getValue().getRangos()).hasSize(3);
        assertThat(solicitud.getValue().getRangos().get(0).getCategoria()).isEqualTo(CategoriaRiesgo.ALTO);
        assertThat(solicitud.getValue().getRangos().get(0).getLimiteInferior()).isEqualTo(-100);
    }

    @Test
    void unAnalistaNoDebePublicarPoliticas() throws Exception {
        MvcResult resultado = mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON).content(POLITICA_VALIDA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"traceId\":\"" + resultado.getResponse().getHeader("X-Trace-Id") + "\"");
        verifyNoInteractions(publicarPoliticaClasificacionUseCase);
    }

    @Test
    void publicarSinTokenDebeResponder401() throws Exception {
        mockMvc.perform(post(RUTA_PUBLICAR).contentType(MediaType.APPLICATION_JSON).content(POLITICA_VALIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));

        verifyNoInteractions(publicarPoliticaClasificacionUseCase);
    }

    @Test
    void unaPoliticaQueNoSuperaLasValidacionesDebeResponder400ConTodosLosErrores() throws Exception {
        when(publicarPoliticaClasificacionUseCase.publicar(any())).thenThrow(new PoliticaClasificacionInvalidaException(
                List.of("La categoría BAJO está duplicada", "Falta el rango de la categoría MEDIO")));

        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(POLITICA_VALIDA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("POLITICA_CLASIFICACION_INVALIDA"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0]").value("La categoría BAJO está duplicada"))
                .andExpect(jsonPath("$.details[1]").value("Falta el rango de la categoría MEDIO"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unaPublicacionConcurrenteDebeResponder409() throws Exception {
        when(publicarPoliticaClasificacionUseCase.publicar(any()))
                .thenThrow(new PublicacionPoliticaConcurrenteException());

        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(POLITICA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PUBLICACION_CONCURRENTE"));
    }

    @Test
    void unaPublicacionSinAutorAutenticadoDebeResponder401() throws Exception {
        when(publicarPoliticaClasificacionUseCase.publicar(any())).thenThrow(new PublicacionSinAutorException());

        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(POLITICA_VALIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unLimiteDecimalDebeResponder400SinLlegarAlCasoDeUso() throws Exception {
        String conDecimal = POLITICA_VALIDA.replace("\"limiteSuperior\": 79", "\"limiteSuperior\": 79.5");

        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(conDecimal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(publicarPoliticaClasificacionUseCase);
    }

    @Test
    void sinRangosDebeResponder400SinLlegarAlCasoDeUso() throws Exception {
        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(publicarPoliticaClasificacionUseCase);
    }

    @Test
    void unaCategoriaFueraDelModeloDebeResponder400SinLlegarAlCasoDeUso() throws Exception {
        String categoriaInvalida = POLITICA_VALIDA.replace("\"MEDIO\"", "\"CRITICO\"");

        mockMvc.perform(post(RUTA_PUBLICAR).with(conRol(JwtConfig.ROL_ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(categoriaInvalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(publicarPoliticaClasificacionUseCase);
    }
}
