package com.crediticio.applicants.application;

import com.crediticio.applicants.application.dto.SolicitanteDetalleResponse;
import com.crediticio.applicants.domain.HistorialCrediticio;
import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.domain.SolicitanteNoEncontradoException;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarSolicitanteServiceTest {

    @Mock
    private SolicitanteRepositoryPort solicitanteRepositoryPort;

    private ConsultarSolicitanteService consultarSolicitanteService;

    @BeforeEach
    void inicializarServicio() {
        consultarSolicitanteService = new ConsultarSolicitanteService(solicitanteRepositoryPort, new SolicitanteMapper());
    }

    @Test
    void debeRetornarElDetalleCuandoElSolicitanteExiste() {
        Solicitante solicitante = Solicitante.reconstruir(
                1L,
                "Ana María Pérez",
                "1234567",
                new BigDecimal("3000000"),
                new BigDecimal("500000"),
                0,
                HistorialCrediticio.BUENO,
                new BigDecimal("2.5"),
                LocalDateTime.of(2026, 1, 1, 10, 0));
        when(solicitanteRepositoryPort.buscarPorNumeroDocumento("1234567")).thenReturn(Optional.of(solicitante));

        SolicitanteDetalleResponse response = consultarSolicitanteService.consultarPorNumeroDocumento("1234567");

        assertThat(response.getIdSolicitante()).isEqualTo(1L);
        assertThat(response.getNombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(response.getNumeroDocumento()).isEqualTo("1234567");
        assertThat(response.getIngresosMensuales()).isEqualByComparingTo(new BigDecimal("3000000"));
        assertThat(response.getDeudasMensuales()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(response.getNumeroMoras()).isZero();
        assertThat(response.getHistorialCrediticio()).isEqualTo(HistorialCrediticio.BUENO);
        assertThat(response.getAntiguedadLaboral()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    void debeLanzarExcepcionCuandoElSolicitanteNoExiste() {
        when(solicitanteRepositoryPort.buscarPorNumeroDocumento("9999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultarSolicitanteService.consultarPorNumeroDocumento("9999999"))
                .isInstanceOf(SolicitanteNoEncontradoException.class);
    }
}
