package com.crediticio.scoring.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangoClasificacionTest {

    @Test
    void reconstruirDebeConservarTodosLosCampos() {
        RangoClasificacion rango = RangoClasificacion.reconstruir(7L, CategoriaRiesgo.MEDIO, 30, 79);

        assertThat(rango.getIdRango()).isEqualTo(7L);
        assertThat(rango.getCategoria()).isEqualTo(CategoriaRiesgo.MEDIO);
        assertThat(rango.getLimiteInferior()).isEqualTo(30);
        assertThat(rango.getLimiteSuperior()).isEqualTo(79);
    }

    @Test
    void debeAceptarLosLimitesGlobalesInclusivos() {
        RangoClasificacion rango = RangoClasificacion.reconstruir(7L, CategoriaRiesgo.BAJO, -100, 130);

        assertThat(rango.getLimiteInferior()).isEqualTo(-100);
        assertThat(rango.getLimiteSuperior()).isEqualTo(130);
    }

    @Test
    void contieneDebeIncluirAmbosLimitesYExcluirLosValoresAdyacentes() {
        RangoClasificacion rango = RangoClasificacion.reconstruir(7L, CategoriaRiesgo.MEDIO, 30, 79);

        assertThat(rango.contiene(30)).isTrue();
        assertThat(rango.contiene(55)).isTrue();
        assertThat(rango.contiene(79)).isTrue();
        assertThat(rango.contiene(29)).isFalse();
        assertThat(rango.contiene(80)).isFalse();
    }

    @Test
    void contieneDebeAceptarUnRangoDeUnSoloValor() {
        RangoClasificacion rango = RangoClasificacion.reconstruir(7L, CategoriaRiesgo.ALTO, 130, 130);

        assertThat(rango.contiene(130)).isTrue();
        assertThat(rango.contiene(129)).isFalse();
    }

    @Test
    void reconstruirDebeRechazarIdRangoNulo() {
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(null, CategoriaRiesgo.BAJO, -100, 29))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarCategoriaNula() {
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(7L, null, -100, 29))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarLimitesNulos() {
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(7L, CategoriaRiesgo.BAJO, null, 29))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(7L, CategoriaRiesgo.BAJO, -100, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarLimitesFueraDelIntervaloOInvertidos() {
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(7L, CategoriaRiesgo.BAJO, -101, 29))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(7L, CategoriaRiesgo.BAJO, 80, 131))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RangoClasificacion.reconstruir(7L, CategoriaRiesgo.BAJO, 79, 30))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
