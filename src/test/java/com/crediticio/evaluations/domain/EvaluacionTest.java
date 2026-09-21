package com.crediticio.evaluations.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluacionTest {

    @Test
    void debeCrearEvaluacionNuevaConEstadoInicialSinIdNiFecha() {
        Evaluacion evaluacion = Evaluacion.nueva(1L, List.of());

        assertThat(evaluacion.getIdEvaluacion()).isNull();
        assertThat(evaluacion.getIdSolicitante()).isEqualTo(1L);
        assertThat(evaluacion.getFechaEvaluacion()).isNull();
        assertThat(evaluacion.getDetalles()).isEmpty();
    }

    @Test
    void scoreTotalDebeSerCeroCuandoNoHayDetalles() {
        Evaluacion evaluacion = Evaluacion.nueva(1L, List.of());

        assertThat(evaluacion.getScoreTotal()).isZero();
    }

    @Test
    void scoreTotalDebeSumarUnicamenteLosPuntajesObtenidosDeLosDetallesCumplidos() {
        List<DetalleEvaluacion> detalles = List.of(
                DetalleEvaluacion.nuevo(1L, ">=", "3000000", 20, true, 20),
                DetalleEvaluacion.nuevo(2L, "<", "5", 10, false, 0),
                DetalleEvaluacion.nuevo(3L, "=", "BUENO", 30, true, 30));

        Evaluacion evaluacion = Evaluacion.nueva(1L, detalles);

        assertThat(evaluacion.getScoreTotal()).isEqualTo(50);
    }

    @Test
    void scoreTotalDebeAdmitirPuntajesNegativos() {
        List<DetalleEvaluacion> detalles = List.of(
                DetalleEvaluacion.nuevo(1L, "=", "MALO", -30, true, -30),
                DetalleEvaluacion.nuevo(2L, ">=", "3000000", 20, true, 20));

        Evaluacion evaluacion = Evaluacion.nueva(1L, detalles);

        assertThat(evaluacion.getScoreTotal()).isEqualTo(-10);
    }

    @Test
    void debeRechazarIdSolicitanteNulo() {
        assertThatThrownBy(() -> Evaluacion.nueva(null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reconstruirDebePreservarTodosLosCampos() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 1, 10, 0);
        List<DetalleEvaluacion> detalles = List.of(DetalleEvaluacion.reconstruir(1L, 1L, "=", "10", 20, true, 20));

        Evaluacion evaluacion = Evaluacion.reconstruir(5L, 1L, fecha, detalles);

        assertThat(evaluacion.getIdEvaluacion()).isEqualTo(5L);
        assertThat(evaluacion.getFechaEvaluacion()).isEqualTo(fecha);
        assertThat(evaluacion.getDetalles()).hasSize(1);
    }
}
