package com.crediticio.applicants.application;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteResponse;
import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.applicants.domain.HistorialCrediticio;
import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarSolicitanteServiceTest {

    @Mock
    private SolicitanteRepositoryPort solicitanteRepositoryPort;

    private RegistrarSolicitanteService registrarSolicitanteService;

    @BeforeEach
    void inicializarServicio() {
        registrarSolicitanteService = new RegistrarSolicitanteService(solicitanteRepositoryPort, new SolicitanteMapper());
    }

    @Test
    void debeRegistrarSolicitanteYRetornarRespuestaConIdGenerado() {
        RegistrarSolicitanteRequest request = solicitudValida();

        when(solicitanteRepositoryPort.existePorNumeroDocumento(request.getNumeroDocumento())).thenReturn(false);
        when(solicitanteRepositoryPort.guardar(any(Solicitante.class))).thenAnswer(invocation -> {
            Solicitante enviado = invocation.getArgument(0);
            return Solicitante.reconstruir(
                    42L,
                    enviado.getNombreCompleto(),
                    enviado.getNumeroDocumento(),
                    enviado.getIngresosMensuales(),
                    enviado.getDeudasMensuales(),
                    enviado.getNumeroMoras(),
                    enviado.getHistorialCrediticio(),
                    enviado.getAntiguedadLaboral(),
                    LocalDateTime.of(2026, 1, 1, 10, 0));
        });

        SolicitanteResponse response = registrarSolicitanteService.registrar(request);

        assertThat(response.getIdSolicitante()).isEqualTo(42L);
        assertThat(response.getNombreCompleto()).isEqualTo(request.getNombreCompleto());
        assertThat(response.getNumeroDocumento()).isEqualTo(request.getNumeroDocumento());
        assertThat(response.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));

        ArgumentCaptor<Solicitante> captor = ArgumentCaptor.forClass(Solicitante.class);
        verify(solicitanteRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getIngresosMensuales()).isEqualTo(request.getIngresosMensuales());
        assertThat(captor.getValue().getDeudasMensuales()).isEqualTo(request.getDeudasMensuales());
        assertThat(captor.getValue().getNumeroMoras()).isEqualTo(request.getNumeroMoras());
        assertThat(captor.getValue().getHistorialCrediticio()).isEqualTo(request.getHistorialCrediticio());
        assertThat(captor.getValue().getAntiguedadLaboral()).isEqualTo(request.getAntiguedadLaboral());
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiElDocumentoYaExiste() {
        RegistrarSolicitanteRequest request = solicitudValida();

        when(solicitanteRepositoryPort.existePorNumeroDocumento(request.getNumeroDocumento())).thenReturn(true);

        assertThatThrownBy(() -> registrarSolicitanteService.registrar(request))
                .isInstanceOf(DocumentoDuplicadoException.class);

        verify(solicitanteRepositoryPort, never()).guardar(any(Solicitante.class));
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
