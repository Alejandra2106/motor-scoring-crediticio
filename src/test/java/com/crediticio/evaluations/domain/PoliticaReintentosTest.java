package com.crediticio.evaluations.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU14 CA14: espera creciente con tope, variación aleatoria y máximo de intentos (valores aprobados). */
class PoliticaReintentosTest {

    private static PoliticaReintentos politica(double aleatorio) {
        return new PoliticaReintentos(10, Duration.ofSeconds(5), Duration.ofSeconds(300), 0.2,
                Duration.ofSeconds(30), () -> aleatorio);
    }

    @Test
    void sinVariacionLaEsperaDebeDuplicarseHastaElTope() {
        PoliticaReintentos sinVariacion = politica(0.5);

        assertThat(sinVariacion.esperaTras(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(sinVariacion.esperaTras(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(sinVariacion.esperaTras(6)).isEqualTo(Duration.ofSeconds(160));
        assertThat(sinVariacion.esperaTras(7)).isEqualTo(Duration.ofSeconds(300));
        assertThat(sinVariacion.esperaTras(9)).isEqualTo(Duration.ofSeconds(300));
        assertThat(sinVariacion.esperaTras(100)).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    void laVariacionDebeQuedarDentroDeMasMenosVeintePorCientoDespuesDelTope() {
        assertThat(politica(0.0).esperaTras(1)).isEqualTo(Duration.ofSeconds(4));
        assertThat(politica(0.999999).esperaTras(1).toMillis()).isBetween(5999L, 6000L);
        assertThat(politica(0.0).esperaTras(8)).isEqualTo(Duration.ofSeconds(240));
        assertThat(politica(0.999999).esperaTras(8).toMillis()).isBetween(359_999L, 360_000L);
    }

    @Test
    void debeAdmitirReintentosHastaElMaximoIncluyendoElIntentoEnLinea() {
        PoliticaReintentos reintentos = politica(0.5);

        assertThat(reintentos.admiteOtroIntento(1)).isTrue();
        assertThat(reintentos.admiteOtroIntento(9)).isTrue();
        assertThat(reintentos.admiteOtroIntento(10)).isFalse();
        assertThat(reintentos.getConcesion()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void debeRechazarValoresInvalidos() {
        assertThatThrownBy(() -> new PoliticaReintentos(0, Duration.ofSeconds(5), Duration.ofSeconds(300), 0.2,
                Duration.ofSeconds(30), () -> 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PoliticaReintentos(10, Duration.ZERO, Duration.ofSeconds(300), 0.2,
                Duration.ofSeconds(30), () -> 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> politica(0.5).esperaTras(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
