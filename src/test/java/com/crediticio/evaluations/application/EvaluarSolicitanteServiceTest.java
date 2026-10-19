package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.domain.ClasificacionEvaluacion;
import com.crediticio.evaluations.domain.EstadoEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU14 CA01, CA13: el intento en línea ocurre después de confirmar el registro y define el estado devuelto. */
class EvaluarSolicitanteServiceTest {

    private static final CalcularScoreRequest SOLICITUD = new CalcularScoreRequest(1L, null);
    private static final UUID TOKEN = UUID.randomUUID();

    private final CalcularScoreService calcularScoreService = mock(CalcularScoreService.class);
    private final ProcesoGeneracionRecomendaciones proceso = mock(ProcesoGeneracionRecomendaciones.class);
    private final EvaluarSolicitanteService servicio = new EvaluarSolicitanteService(calcularScoreService, proceso,
            new EvaluacionMapper());

    private static EvaluacionRegistrada registrada() {
        Evaluacion pendiente = Evaluacion.reconstruir(7L, 1L, LocalDateTime.now(), 0,
                new ClasificacionEvaluacion("BAJO", 3L, 2), EstadoEvaluacion.PENDIENTE, List.of());
        return new EvaluacionRegistrada(pendiente, TOKEN);
    }

    @Test
    void conElIntentoEnLineaRegistradoDebeResponderCompletada() {
        when(calcularScoreService.registrar(SOLICITUD)).thenReturn(registrada());
        when(proceso.intentar(7L, TOKEN, ModoIntento.EN_LINEA, 1)).thenReturn(true);

        assertThat(servicio.calcular(SOLICITUD).getEstado()).isEqualTo("COMPLETADA");
    }

    @Test
    void siElIntentoEnLineaNoSeCompletaDebeResponderPendiente() {
        when(calcularScoreService.registrar(SOLICITUD)).thenReturn(registrada());
        when(proceso.intentar(7L, TOKEN, ModoIntento.EN_LINEA, 1)).thenReturn(false);

        assertThat(servicio.calcular(SOLICITUD).getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void siElRegistroFallaNoDebeIntentarseLaGeneracion() {
        when(calcularScoreService.registrar(SOLICITUD)).thenThrow(new IllegalStateException("fallo"));

        assertThatThrownBy(() -> servicio.calcular(SOLICITUD)).isInstanceOf(IllegalStateException.class);
        verify(proceso, never()).intentar(anyLong(), any(), any(), anyInt());
    }
}
