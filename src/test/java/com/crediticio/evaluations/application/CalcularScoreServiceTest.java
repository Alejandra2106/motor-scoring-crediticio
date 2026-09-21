package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.domain.NivelEndeudamientoIndeterminadoException;
import com.crediticio.evaluations.domain.NombreVariableEvaluada;
import com.crediticio.evaluations.domain.SolicitanteEvaluacionNoEncontradoException;
import com.crediticio.evaluations.domain.SolicitanteParaEvaluacion;
import com.crediticio.evaluations.ports.output.ConsultarSolicitanteEvaluacionPort;
import com.crediticio.evaluations.ports.output.ConsultarVariablesActivasEvaluacionPort;
import com.crediticio.evaluations.ports.output.EvaluacionRepositoryPort;
import com.crediticio.evaluations.ports.output.ReglasScoringConsultaPort;
import com.crediticio.evaluations.ports.output.ResultadoReglaEvaluada;
import com.crediticio.evaluations.ports.output.VariableActivaEvaluada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalcularScoreServiceTest {

    @Mock
    private ConsultarSolicitanteEvaluacionPort consultarSolicitantePort;

    @Mock
    private ConsultarVariablesActivasEvaluacionPort consultarVariablesActivasPort;

    @Mock
    private ReglasScoringConsultaPort reglasScoringConsultaPort;

    @Mock
    private EvaluacionRepositoryPort evaluacionRepositoryPort;

    private CalcularScoreService service;

    @BeforeEach
    void inicializarServicio() {
        service = new CalcularScoreService(consultarSolicitantePort, consultarVariablesActivasPort,
                reglasScoringConsultaPort, evaluacionRepositoryPort, new EvaluacionMapper());
    }

    @Test
    void debeLanzarExcepcionYNoGuardarCuandoElSolicitanteNoExistePorId() {
        when(consultarSolicitantePort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(new CalcularScoreRequest(99L, null)))
                .isInstanceOf(SolicitanteEvaluacionNoEncontradoException.class);

        verify(evaluacionRepositoryPort, never()).guardar(any(Evaluacion.class));
    }

    @Test
    void debeLanzarExcepcionYNoGuardarCuandoElSolicitanteNoExistePorDocumento() {
        when(consultarSolicitantePort.buscarPorNumeroDocumento("1234567890")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(new CalcularScoreRequest(null, "1234567890")))
                .isInstanceOf(SolicitanteEvaluacionNoEncontradoException.class);

        verify(evaluacionRepositoryPort, never()).guardar(any(Evaluacion.class));
    }

    @Test
    void debeIdentificarElSolicitantePorIdCuandoSeProporciona() {
        when(consultarSolicitantePort.buscarPorId(1L)).thenReturn(Optional.of(solicitante(1L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(List.of());
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        service.calcular(new CalcularScoreRequest(1L, null));

        verify(consultarSolicitantePort).buscarPorId(1L);
        verify(consultarSolicitantePort, never()).buscarPorNumeroDocumento(any());
    }

    @Test
    void debeIdentificarElSolicitantePorDocumentoCuandoSeProporciona() {
        when(consultarSolicitantePort.buscarPorNumeroDocumento("1234567890")).thenReturn(Optional.of(solicitante(1L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(List.of());
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        service.calcular(new CalcularScoreRequest(null, "1234567890"));

        verify(consultarSolicitantePort).buscarPorNumeroDocumento("1234567890");
        verify(consultarSolicitantePort, never()).buscarPorId(any());
    }

    @Test
    void debeCrearEvaluacionSinDetallesCuandoNoHayVariablesActivas() {
        when(consultarSolicitantePort.buscarPorId(1L)).thenReturn(Optional.of(solicitante(1L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(List.of());
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        EvaluacionResponse response = service.calcular(new CalcularScoreRequest(1L, null));

        assertThat(response.getScoreTotal()).isZero();
        assertThat(response.getDetalles()).isEmpty();
        verify(reglasScoringConsultaPort, never()).idsConReglasVigentes(anySet());
    }

    @Test
    void debeCrearEvaluacionSinDetallesCuandoLaVariableActivaNoTieneReglas() {
        when(consultarSolicitantePort.buscarPorId(1L)).thenReturn(Optional.of(solicitante(1L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(
                List.of(new VariableActivaEvaluada(1L, NombreVariableEvaluada.INGRESOS_MENSUALES)));
        when(reglasScoringConsultaPort.idsConReglasVigentes(Set.of(1L))).thenReturn(Set.of());
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        EvaluacionResponse response = service.calcular(new CalcularScoreRequest(1L, null));

        assertThat(response.getDetalles()).isEmpty();
        verify(reglasScoringConsultaPort, never()).evaluar(anyMap());
    }

    @Test
    void debeCalcularElScoreTotalSumandoLosPuntajesDeLasReglasCumplidas() {
        when(consultarSolicitantePort.buscarPorId(1L)).thenReturn(Optional.of(solicitante(1L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(List.of(
                new VariableActivaEvaluada(1L, NombreVariableEvaluada.INGRESOS_MENSUALES),
                new VariableActivaEvaluada(4L, NombreVariableEvaluada.HISTORIAL_CREDITICIO)));
        when(reglasScoringConsultaPort.idsConReglasVigentes(Set.of(1L, 4L))).thenReturn(Set.of(1L, 4L));
        when(reglasScoringConsultaPort.evaluar(anyMap())).thenReturn(List.of(
                new ResultadoReglaEvaluada(10L, ">=", "3000000", 20, true, 20),
                new ResultadoReglaEvaluada(11L, "=", "MALO", 30, false, 0)));
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        EvaluacionResponse response = service.calcular(new CalcularScoreRequest(1L, null));

        assertThat(response.getScoreTotal()).isEqualTo(20);
        assertThat(response.getDetalles()).hasSize(2);
    }

    @Test
    void debeResolverElValorRealSoloParaLasVariablesConReglasVigentes() {
        when(consultarSolicitantePort.buscarPorId(1L)).thenReturn(Optional.of(solicitante(1L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(List.of(
                new VariableActivaEvaluada(1L, NombreVariableEvaluada.INGRESOS_MENSUALES),
                new VariableActivaEvaluada(2L, NombreVariableEvaluada.NIVEL_ENDEUDAMIENTO)));
        // Solo la variable 1 tiene reglas vigentes: NIVEL_ENDEUDAMIENTO (variable 2, sin reglas)
        // nunca debería intentar resolverse, evitando así un bloqueo D8 innecesario.
        when(reglasScoringConsultaPort.idsConReglasVigentes(Set.of(1L, 2L))).thenReturn(Set.of(1L));
        when(reglasScoringConsultaPort.evaluar(Map.of(1L, "4000000"))).thenReturn(List.of(
                new ResultadoReglaEvaluada(10L, ">=", "3000000", 20, true, 20)));
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        EvaluacionResponse response = service.calcular(new CalcularScoreRequest(1L, null));

        assertThat(response.getScoreTotal()).isEqualTo(20);
    }

    @Test
    void debePropagarNivelEndeudamientoIndeterminadoYNoGuardarNingunaEvaluacion() {
        SolicitanteParaEvaluacion solicitanteIngresosCero = new SolicitanteParaEvaluacion(
                1L, BigDecimal.ZERO, new BigDecimal("500000"), 0, "BUENO", new BigDecimal("1"));
        when(consultarSolicitantePort.buscarPorId(1L)).thenReturn(Optional.of(solicitanteIngresosCero));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(
                List.of(new VariableActivaEvaluada(2L, NombreVariableEvaluada.NIVEL_ENDEUDAMIENTO)));
        when(reglasScoringConsultaPort.idsConReglasVigentes(Set.of(2L))).thenReturn(Set.of(2L));

        assertThatThrownBy(() -> service.calcular(new CalcularScoreRequest(1L, null)))
                .isInstanceOf(NivelEndeudamientoIndeterminadoException.class);

        verify(evaluacionRepositoryPort, never()).guardar(any(Evaluacion.class));
    }

    @Test
    void debeGuardarLaEvaluacionConElIdSolicitanteCorrecto() {
        when(consultarSolicitantePort.buscarPorId(7L)).thenReturn(Optional.of(solicitante(7L)));
        when(consultarVariablesActivasPort.listarActivas()).thenReturn(List.of());
        when(evaluacionRepositoryPort.guardar(any(Evaluacion.class)))
                .thenAnswer(invocation -> reconstruirConId(invocation.getArgument(0), 100L));

        service.calcular(new CalcularScoreRequest(7L, null));

        ArgumentCaptor<Evaluacion> captor = ArgumentCaptor.forClass(Evaluacion.class);
        verify(evaluacionRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getIdSolicitante()).isEqualTo(7L);
    }

    private SolicitanteParaEvaluacion solicitante(Long idSolicitante) {
        return new SolicitanteParaEvaluacion(idSolicitante, new BigDecimal("4000000"), new BigDecimal("1200000"),
                0, "BUENO", new BigDecimal("2"));
    }

    private Evaluacion reconstruirConId(Evaluacion evaluacion, Long idEvaluacion) {
        return Evaluacion.reconstruir(idEvaluacion, evaluacion.getIdSolicitante(), LocalDateTime.now(),
                evaluacion.getDetalles());
    }
}
