package com.crediticio.scoring.infrastructure.output;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.ports.output.VariableRiesgoConsultada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariableRiesgoConsultaAdapterTest {

    @Mock
    private VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    private VariableRiesgoConsultaAdapter adapter;

    @Test
    void debeRetornarVacioCuandoLaVariableNoExiste() {
        adapter = new VariableRiesgoConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThat(adapter.consultar(99L)).isEmpty();
    }

    @Test
    void debeMapearVariableNumericaActivaConDecimalesPermitidos() {
        adapter = new VariableRiesgoConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(
                VariableRiesgo.reconstruir(1L, NombreVariableRiesgo.INGRESOS_MENSUALES,
                        "Ingresos mensuales netos declarados por el solicitante",
                        EstadoVariableRiesgo.ACTIVA, null)));

        VariableRiesgoConsultada consultada = adapter.consultar(1L).orElseThrow();

        assertThat(consultada.activa()).isTrue();
        assertThat(consultada.tipo()).isEqualTo(TipoVariable.NUMERICO);
        assertThat(consultada.permiteDecimales()).isTrue();
    }

    @Test
    void debeMapearNumeroMorasComoNumericoSinDecimalesPermitidos() {
        adapter = new VariableRiesgoConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.buscarPorId(2L)).thenReturn(Optional.of(
                VariableRiesgo.reconstruir(2L, NombreVariableRiesgo.NUMERO_MORAS,
                        "Número de moras registradas en el historial crediticio",
                        EstadoVariableRiesgo.ACTIVA, null)));

        VariableRiesgoConsultada consultada = adapter.consultar(2L).orElseThrow();

        assertThat(consultada.tipo()).isEqualTo(TipoVariable.NUMERICO);
        assertThat(consultada.permiteDecimales()).isFalse();
    }

    @Test
    void debeMapearHistorialCrediticioComoCategorico() {
        adapter = new VariableRiesgoConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.buscarPorId(3L)).thenReturn(Optional.of(
                VariableRiesgo.reconstruir(3L, NombreVariableRiesgo.HISTORIAL_CREDITICIO,
                        "Historial crediticio consolidado del solicitante evaluado",
                        EstadoVariableRiesgo.ACTIVA, null)));

        VariableRiesgoConsultada consultada = adapter.consultar(3L).orElseThrow();

        assertThat(consultada.tipo()).isEqualTo(TipoVariable.CATEGORICO);
    }

    @Test
    void debeMapearVariableInactivaComoNoActiva() {
        adapter = new VariableRiesgoConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.buscarPorId(4L)).thenReturn(Optional.of(
                VariableRiesgo.reconstruir(4L, NombreVariableRiesgo.ANTIGUEDAD_LABORAL,
                        "Antigüedad laboral acumulada en el empleo actual",
                        EstadoVariableRiesgo.INACTIVA, null)));

        VariableRiesgoConsultada consultada = adapter.consultar(4L).orElseThrow();

        assertThat(consultada.activa()).isFalse();
    }
}
