package com.crediticio.evaluations.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasificacionEvaluacionTest {

    @Test
    void debeConservarCategoriaYReferenciaALaVersionDeLaPolitica() {
        ClasificacionEvaluacion clasificacion = new ClasificacionEvaluacion("BAJO", 4L, 3);

        assertThat(clasificacion.categoria()).isEqualTo("BAJO");
        assertThat(clasificacion.idPolitica()).isEqualTo(4L);
        assertThat(clasificacion.versionPolitica()).isEqualTo(3);
    }

    @Test
    void debeRechazarUnaCategoriaAusente() {
        assertThatThrownBy(() -> new ClasificacionEvaluacion(null, 4L, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasificacionEvaluacion(" ", 4L, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeExigirLaReferenciaALaPolitica() {
        assertThatThrownBy(() -> new ClasificacionEvaluacion("BAJO", null, 3)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClasificacionEvaluacion("BAJO", 4L, null))
                .isInstanceOf(NullPointerException.class);
    }
}
