package com.crediticio.scoring.ports.output;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaClasificacionRepositoryPortTest {

    @Test
    void elPuertoSoloDebeOfrecerLaPublicacionSinOperacionesDeModificacionNiEliminacion() {
        assertThat(Arrays.stream(PoliticaClasificacionRepositoryPort.class.getDeclaredMethods())
                .map(Method::getName))
                .containsExactly("publicar");
    }
}
