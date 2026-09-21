package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.applicants.domain.HistorialCrediticio;
import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import com.crediticio.evaluations.domain.SolicitanteParaEvaluacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitanteConsultaAdapterTest {

    @Mock
    private SolicitanteRepositoryPort solicitanteRepositoryPort;

    private SolicitanteConsultaAdapter adapter;

    @Test
    void buscarPorIdDebeRetornarVacioCuandoElSolicitanteNoExiste() {
        adapter = new SolicitanteConsultaAdapter(solicitanteRepositoryPort);
        when(solicitanteRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThat(adapter.buscarPorId(99L)).isEmpty();
    }

    @Test
    void buscarPorIdDebeMapearTodosLosCamposNecesariosParaLaEvaluacion() {
        adapter = new SolicitanteConsultaAdapter(solicitanteRepositoryPort);
        Solicitante solicitante = Solicitante.reconstruir(1L, "Ana Pérez", "1234567890",
                new BigDecimal("4000000"), new BigDecimal("1200000"), 2, HistorialCrediticio.BUENO,
                new BigDecimal("3.5"), null);
        when(solicitanteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(solicitante));

        SolicitanteParaEvaluacion proyeccion = adapter.buscarPorId(1L).orElseThrow();

        assertThat(proyeccion.idSolicitante()).isEqualTo(1L);
        assertThat(proyeccion.ingresosMensuales()).isEqualTo(new BigDecimal("4000000"));
        assertThat(proyeccion.deudasMensuales()).isEqualTo(new BigDecimal("1200000"));
        assertThat(proyeccion.numeroMoras()).isEqualTo(2);
        assertThat(proyeccion.historialCrediticio()).isEqualTo("BUENO");
        assertThat(proyeccion.antiguedadLaboral()).isEqualTo(new BigDecimal("3.5"));
    }

    @Test
    void buscarPorNumeroDocumentoDebeMapearElSolicitanteEncontrado() {
        adapter = new SolicitanteConsultaAdapter(solicitanteRepositoryPort);
        Solicitante solicitante = Solicitante.reconstruir(2L, "Luis Gómez", "9876543210",
                new BigDecimal("5000000"), new BigDecimal("0"), 0, HistorialCrediticio.REGULAR,
                new BigDecimal("1"), null);
        when(solicitanteRepositoryPort.buscarPorNumeroDocumento("9876543210")).thenReturn(Optional.of(solicitante));

        SolicitanteParaEvaluacion proyeccion = adapter.buscarPorNumeroDocumento("9876543210").orElseThrow();

        assertThat(proyeccion.idSolicitante()).isEqualTo(2L);
        assertThat(proyeccion.historialCrediticio()).isEqualTo("REGULAR");
    }
}
