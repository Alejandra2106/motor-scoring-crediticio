package com.crediticio.evaluations.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetalleEvaluacionTest {

    @Test
    void debeCrearDetalleConCondicionCumplidaYPuntajeObtenidoIgualAlDeLaRegla() {
        DetalleEvaluacion detalle = DetalleEvaluacion.nuevo(1L, ">=", "3000000", 20, true, 20);

        assertThat(detalle.getIdRegla()).isEqualTo(1L);
        assertThat(detalle.getOperadorAplicado()).isEqualTo(">=");
        assertThat(detalle.getValorCondicionAplicado()).isEqualTo("3000000");
        assertThat(detalle.getPuntajeReglaAplicado()).isEqualTo(20);
        assertThat(detalle.isCondicionCumplida()).isTrue();
        assertThat(detalle.getPuntajeObtenido()).isEqualTo(20);
    }

    @Test
    void debeCrearDetalleConCondicionNoCumplidaYPuntajeObtenidoEnCero() {
        DetalleEvaluacion detalle = DetalleEvaluacion.nuevo(2L, "<", "10", 15, false, 0);

        assertThat(detalle.isCondicionCumplida()).isFalse();
        assertThat(detalle.getPuntajeObtenido()).isZero();
    }

    @Test
    void debeAceptarPuntajeReglaNegativoCuandoLaCondicionSeCumple() {
        DetalleEvaluacion detalle = DetalleEvaluacion.nuevo(3L, "=", "MALO", -30, true, -30);

        assertThat(detalle.getPuntajeObtenido()).isEqualTo(-30);
    }

    @Test
    void debeRechazarPuntajeObtenidoInconsistenteCuandoLaCondicionSeCumple() {
        assertThatThrownBy(() -> DetalleEvaluacion.nuevo(1L, "=", "10", 20, true, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarPuntajeObtenidoInconsistenteCuandoLaCondicionNoSeCumple() {
        assertThatThrownBy(() -> DetalleEvaluacion.nuevo(1L, "=", "10", 20, false, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarIdReglaNulo() {
        assertThatThrownBy(() -> DetalleEvaluacion.nuevo(null, "=", "10", 20, true, 20))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarOperadorAplicadoEnBlanco() {
        assertThatThrownBy(() -> DetalleEvaluacion.nuevo(1L, "  ", "10", 20, true, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstruirDebePreservarTodosLosCamposIncluidoElId() {
        DetalleEvaluacion detalle = DetalleEvaluacion.reconstruir(99L, 1L, ">=", "3000000", 20, true, 20);

        assertThat(detalle.getIdDetalleEvaluacion()).isEqualTo(99L);
        assertThat(detalle.getIdRegla()).isEqualTo(1L);
    }
}
