package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.EditarReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.domain.EstadoReglaScoring;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.domain.ReglaScoringNoEncontradaException;
import com.crediticio.scoring.domain.ReglaScoringVariableInactivaException;
import com.crediticio.scoring.domain.ReglaScoringVariableNoEncontradaException;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.ports.output.ConsultarVariableRiesgoPort;
import com.crediticio.scoring.ports.output.ReglaScoringRepositoryPort;
import com.crediticio.scoring.ports.output.VariableRiesgoConsultada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditarReglaScoringServiceTest {

    @Mock
    private ConsultarVariableRiesgoPort consultarVariableRiesgoPort;

    @Mock
    private ReglaScoringRepositoryPort reglaScoringRepositoryPort;

    private EditarReglaScoringService editarReglaScoringService;

    @BeforeEach
    void inicializarServicio() {
        editarReglaScoringService = new EditarReglaScoringService(
                consultarVariableRiesgoPort, reglaScoringRepositoryPort, new ReglaScoringMapper());
    }

    @Test
    void debeEditarLaReglaPreservandoIdRiesgoYEstadoYRetornarLaRespuestaActualizada() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, null);
        EditarReglaScoringRequest request = new EditarReglaScoringRequest(OperadorScoring.MENOR, "5000000", 25);

        when(reglaScoringRepositoryPort.buscarPorId(7L)).thenReturn(Optional.of(reglaExistente));
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));
        when(reglaScoringRepositoryPort.existeCombinacion(anyLong(), any(OperadorScoring.class), anyString(), anyLong()))
                .thenReturn(false);
        when(reglaScoringRepositoryPort.guardar(any(ReglaScoring.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReglaScoringResponse response = editarReglaScoringService.editar(7L, request);

        assertThat(response.getIdRegla()).isEqualTo(7L);
        assertThat(response.getIdRiesgo()).isEqualTo(1L);
        assertThat(response.getOperador()).isEqualTo(OperadorScoring.MENOR);
        assertThat(response.getValorCondicion()).isEqualTo("5000000");
        assertThat(response.getPuntaje()).isEqualTo(25);

        ArgumentCaptor<ReglaScoring> captor = ArgumentCaptor.forClass(ReglaScoring.class);
        verify(reglaScoringRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getIdRegla()).isEqualTo(7L);
        assertThat(captor.getValue().getIdRiesgo()).isEqualTo(1L);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaReglaNoExiste() {
        when(reglaScoringRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> editarReglaScoringService.editar(99L, solicitudValida()))
                .isInstanceOf(ReglaScoringNoEncontradaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaVariableAsociadaNoExiste() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, null);
        when(reglaScoringRepositoryPort.buscarPorId(7L)).thenReturn(Optional.of(reglaExistente));
        when(consultarVariableRiesgoPort.consultar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> editarReglaScoringService.editar(7L, solicitudValida()))
                .isInstanceOf(ReglaScoringVariableNoEncontradaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaVariableEstaInactiva() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, null);
        when(reglaScoringRepositoryPort.buscarPorId(7L)).thenReturn(Optional.of(reglaExistente));
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(false, TipoVariable.NUMERICO, true)));

        assertThatThrownBy(() -> editarReglaScoringService.editar(7L, solicitudValida()))
                .isInstanceOf(ReglaScoringVariableInactivaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaEdicionDuplicaOtraRegla() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, null);
        when(reglaScoringRepositoryPort.buscarPorId(7L)).thenReturn(Optional.of(reglaExistente));
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));
        when(reglaScoringRepositoryPort.existeCombinacion(1L, OperadorScoring.MENOR, "5000000", 7L))
                .thenReturn(true);

        EditarReglaScoringRequest request = new EditarReglaScoringRequest(OperadorScoring.MENOR, "5000000", 25);

        assertThatThrownBy(() -> editarReglaScoringService.editar(7L, request))
                .isInstanceOf(ReglaScoringDuplicadaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debePermitirConservarLaMismaCombinacionDeLaPropiaRegla() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, null);
        when(reglaScoringRepositoryPort.buscarPorId(7L)).thenReturn(Optional.of(reglaExistente));
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));
        when(reglaScoringRepositoryPort.existeCombinacion(1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 7L))
                .thenReturn(false);
        when(reglaScoringRepositoryPort.guardar(any(ReglaScoring.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EditarReglaScoringRequest request = new EditarReglaScoringRequest(OperadorScoring.MAYOR_O_IGUAL, "3000000", 30);

        ReglaScoringResponse response = editarReglaScoringService.editar(7L, request);

        assertThat(response.getPuntaje()).isEqualTo(30);
    }

    @Test
    void debeRechazarOperadorIncompatibleConVariableCategoricaSinGuardar() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                4L, 4L, OperadorScoring.IGUAL, "BUENO", 30, EstadoReglaScoring.ACTIVA, null);
        when(reglaScoringRepositoryPort.buscarPorId(4L)).thenReturn(Optional.of(reglaExistente));
        when(consultarVariableRiesgoPort.consultar(4L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.CATEGORICO, false)));

        EditarReglaScoringRequest request = new EditarReglaScoringRequest(OperadorScoring.MAYOR_O_IGUAL, "BUENO", 30);

        assertThatThrownBy(() -> editarReglaScoringService.editar(4L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    private EditarReglaScoringRequest solicitudValida() {
        return new EditarReglaScoringRequest(OperadorScoring.MAYOR_O_IGUAL, "3000000", 20);
    }
}
