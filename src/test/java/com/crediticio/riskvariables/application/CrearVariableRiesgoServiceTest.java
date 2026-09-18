package com.crediticio.riskvariables.application;

import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;
import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.TipoVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearVariableRiesgoServiceTest {

    @Mock
    private VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    private CrearVariableRiesgoService crearVariableRiesgoService;

    @BeforeEach
    void inicializarServicio() {
        crearVariableRiesgoService = new CrearVariableRiesgoService(variableRiesgoRepositoryPort, new VariableRiesgoMapper());
    }

    @Test
    void debeCrearVariableRiesgoYRetornarRespuestaConIdGeneradoYEstadoActiva() {
        CrearVariableRiesgoRequest request = solicitudValida();

        when(variableRiesgoRepositoryPort.existePorVariable(request.getVariable())).thenReturn(false);
        when(variableRiesgoRepositoryPort.guardar(any(VariableRiesgo.class))).thenAnswer(invocation -> {
            VariableRiesgo enviada = invocation.getArgument(0);
            return VariableRiesgo.reconstruir(
                    7L,
                    enviada.getVariable(),
                    enviada.getDescripcion(),
                    enviada.getEstado(),
                    LocalDateTime.of(2026, 1, 1, 10, 0));
        });

        VariableRiesgoResponse response = crearVariableRiesgoService.crear(request);

        assertThat(response.getIdVariableRiesgo()).isEqualTo(7L);
        assertThat(response.getVariable()).isEqualTo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        assertThat(response.getTipo()).isEqualTo(TipoVariableRiesgo.NUMERICO);
        assertThat(response.getDescripcion()).isEqualTo(request.getDescripcion());
        assertThat(response.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);

        ArgumentCaptor<VariableRiesgo> captor = ArgumentCaptor.forClass(VariableRiesgo.class);
        verify(variableRiesgoRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getVariable()).isEqualTo(request.getVariable());
        assertThat(captor.getValue().getDescripcion()).isEqualTo(request.getDescripcion());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
    }

    @Test
    void debeLanzarExcepcionYNoGuardarSiLaVariableYaExiste() {
        CrearVariableRiesgoRequest request = solicitudValida();

        when(variableRiesgoRepositoryPort.existePorVariable(request.getVariable())).thenReturn(true);

        assertThatThrownBy(() -> crearVariableRiesgoService.crear(request))
                .isInstanceOf(VariableRiesgoDuplicadaException.class);

        verify(variableRiesgoRepositoryPort, never()).guardar(any(VariableRiesgo.class));
    }

    private CrearVariableRiesgoRequest solicitudValida() {
        return new CrearVariableRiesgoRequest(
                NombreVariableRiesgo.INGRESOS_MENSUALES,
                "Ingresos mensuales netos declarados por el solicitante");
    }
}
