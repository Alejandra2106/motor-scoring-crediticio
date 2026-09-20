package com.crediticio.riskvariables.application;

import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoResponse;
import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoNoEncontradaException;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoVariableRiesgoServiceTest {

    @Mock
    private VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    private CambiarEstadoVariableRiesgoService cambiarEstadoVariableRiesgoService;

    @BeforeEach
    void inicializarServicio() {
        cambiarEstadoVariableRiesgoService = new CambiarEstadoVariableRiesgoService(variableRiesgoRepositoryPort);
    }

    @Test
    void debeCambiarEstadoDeActivaAInactivaYRetornarEstadoAnteriorYNuevo() {
        VariableRiesgo variableExistente = VariableRiesgo.reconstruir(
                25L, NombreVariableRiesgo.INGRESOS_MENSUALES, "Ingresos mensuales netos declarados por el solicitante",
                EstadoVariableRiesgo.ACTIVA, LocalDateTime.of(2026, 1, 1, 10, 0));

        when(variableRiesgoRepositoryPort.buscarPorId(25L)).thenReturn(Optional.of(variableExistente));
        when(variableRiesgoRepositoryPort.guardar(any(VariableRiesgo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CambiarEstadoVariableRiesgoResponse response = cambiarEstadoVariableRiesgoService.cambiarEstado(
                25L, new CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo.INACTIVA));

        assertThat(response.getIdRiesgo()).isEqualTo(25L);
        assertThat(response.getEstadoAnterior()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(response.getEstadoNuevo()).isEqualTo(EstadoVariableRiesgo.INACTIVA);

        ArgumentCaptor<VariableRiesgo> captor = ArgumentCaptor.forClass(VariableRiesgo.class);
        verify(variableRiesgoRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoVariableRiesgo.INACTIVA);
        assertThat(captor.getValue().getIdVariableRiesgo()).isEqualTo(25L);
        assertThat(captor.getValue().getVariable()).isEqualTo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        assertThat(captor.getValue().getDescripcion()).isEqualTo(variableExistente.getDescripcion());
    }

    @Test
    void debeCambiarEstadoDeInactivaAActiva() {
        VariableRiesgo variableExistente = VariableRiesgo.reconstruir(
                30L, NombreVariableRiesgo.NUMERO_MORAS, "Número de moras registradas en el historial crediticio",
                EstadoVariableRiesgo.INACTIVA, LocalDateTime.of(2026, 1, 1, 10, 0));

        when(variableRiesgoRepositoryPort.buscarPorId(30L)).thenReturn(Optional.of(variableExistente));
        when(variableRiesgoRepositoryPort.guardar(any(VariableRiesgo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CambiarEstadoVariableRiesgoResponse response = cambiarEstadoVariableRiesgoService.cambiarEstado(
                30L, new CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo.ACTIVA));

        assertThat(response.getEstadoAnterior()).isEqualTo(EstadoVariableRiesgo.INACTIVA);
        assertThat(response.getEstadoNuevo()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaVariableNoExiste() {
        when(variableRiesgoRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cambiarEstadoVariableRiesgoService.cambiarEstado(
                999L, new CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo.INACTIVA)))
                .isInstanceOf(VariableRiesgoNoEncontradaException.class);

        verify(variableRiesgoRepositoryPort, never()).guardar(any(VariableRiesgo.class));
    }
}
