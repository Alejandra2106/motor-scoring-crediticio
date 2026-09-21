package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.CrearReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.domain.EstadoReglaScoring;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
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

import java.time.LocalDateTime;
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
class CrearReglaScoringServiceTest {

    @Mock
    private ConsultarVariableRiesgoPort consultarVariableRiesgoPort;

    @Mock
    private ReglaScoringRepositoryPort reglaScoringRepositoryPort;

    private CrearReglaScoringService crearReglaScoringService;

    @BeforeEach
    void inicializarServicio() {
        crearReglaScoringService = new CrearReglaScoringService(
                consultarVariableRiesgoPort, reglaScoringRepositoryPort, new ReglaScoringMapper());
    }

    @Test
    void debeCrearReglaYRetornarRespuestaConIdGenerado() {
        CrearReglaScoringRequest request = solicitudNumericaValida();
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));
        when(reglaScoringRepositoryPort.existeCombinacion(anyLong(), any(OperadorScoring.class), anyString()))
                .thenReturn(false);
        when(reglaScoringRepositoryPort.guardar(any(ReglaScoring.class))).thenAnswer(invocation -> {
            ReglaScoring enviada = invocation.getArgument(0);
            return ReglaScoring.reconstruir(9L, enviada.getIdRiesgo(), enviada.getOperador(),
                    enviada.getValorCondicion(), enviada.getPuntaje(), enviada.getEstado(),
                    LocalDateTime.of(2026, 1, 1, 10, 0));
        });

        ReglaScoringResponse response = crearReglaScoringService.crear(request);

        assertThat(response.getIdRegla()).isEqualTo(9L);
        assertThat(response.getIdRiesgo()).isEqualTo(1L);
        assertThat(response.getOperador()).isEqualTo(OperadorScoring.MAYOR_O_IGUAL);
        assertThat(response.getValorCondicion()).isEqualTo("3000000");
        assertThat(response.getPuntaje()).isEqualTo(20);

        ArgumentCaptor<ReglaScoring> captor = ArgumentCaptor.forClass(ReglaScoring.class);
        verify(reglaScoringRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaVariableNoExiste() {
        CrearReglaScoringRequest request = solicitudNumericaValida();
        when(consultarVariableRiesgoPort.consultar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearReglaScoringService.crear(request))
                .isInstanceOf(ReglaScoringVariableNoEncontradaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaVariableEstaInactiva() {
        CrearReglaScoringRequest request = solicitudNumericaValida();
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(false, TipoVariable.NUMERICO, true)));

        assertThatThrownBy(() -> crearReglaScoringService.crear(request))
                .isInstanceOf(ReglaScoringVariableInactivaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaCombinacionYaExiste() {
        CrearReglaScoringRequest request = solicitudNumericaValida();
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));
        when(reglaScoringRepositoryPort.existeCombinacion(anyLong(), any(OperadorScoring.class), anyString()))
                .thenReturn(true);

        assertThatThrownBy(() -> crearReglaScoringService.crear(request))
                .isInstanceOf(ReglaScoringDuplicadaException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    @Test
    void debeRechazarOperadorIncompatibleConVariableCategoricaSinGuardar() {
        CrearReglaScoringRequest request = new CrearReglaScoringRequest(4L, OperadorScoring.MAYOR_O_IGUAL, "BUENO", 30);
        when(consultarVariableRiesgoPort.consultar(4L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.CATEGORICO, false)));

        assertThatThrownBy(() -> crearReglaScoringService.crear(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reglaScoringRepositoryPort, never()).guardar(any(ReglaScoring.class));
    }

    private CrearReglaScoringRequest solicitudNumericaValida() {
        return new CrearReglaScoringRequest(1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20);
    }
}
