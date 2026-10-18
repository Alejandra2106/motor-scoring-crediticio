package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.ResultadoClasificacion;
import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import com.crediticio.scoring.domain.RangoClasificacion;
import com.crediticio.scoring.ports.output.PoliticaClasificacionConsultaPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ClasificacionRiesgoAdapterTest {

    private static final LocalDateTime PUBLICACION = LocalDateTime.of(2026, 10, 1, 9, 0);

    private static PoliticaClasificacion politica(Long idPolitica, Integer version) {
        return PoliticaClasificacion.reconstruir(idPolitica, version, PUBLICACION, PUBLICACION, UUID.randomUUID(),
                List.of(RangoClasificacion.reconstruir(1L, CategoriaRiesgo.ALTO, -100, 39),
                        RangoClasificacion.reconstruir(2L, CategoriaRiesgo.MEDIO, 40, 79),
                        RangoClasificacion.reconstruir(3L, CategoriaRiesgo.BAJO, 80, 130)));
    }

    @ParameterizedTest(name = "puntaje {0} -> {1}")
    @CsvSource({"-100, ALTO", "39, ALTO", "40, MEDIO", "79, MEDIO", "80, BAJO", "130, BAJO"})
    void debeClasificarConLosRangosDeLaPoliticaVigente(int puntaje, String categoria) {
        ClasificacionRiesgoAdapter adapter = new ClasificacionRiesgoAdapter(() -> Optional.of(politica(7L, 3)));

        assertThat(adapter.clasificarConPoliticaVigente(puntaje))
                .contains(new ResultadoClasificacion(7L, 3, categoria));
    }

    @ParameterizedTest(name = "puntaje {0}")
    @CsvSource({"-101", "131"})
    void unPuntajeFueraDeTodosLosRangosDebeQuedarSinCategoriaPeroConLaPoliticaUsada(int puntaje) {
        ClasificacionRiesgoAdapter adapter = new ClasificacionRiesgoAdapter(() -> Optional.of(politica(7L, 3)));

        assertThat(adapter.clasificarConPoliticaVigente(puntaje))
                .contains(new ResultadoClasificacion(7L, 3, null));
    }

    @Test
    void sinPoliticaVigenteDebeRetornarVacio() {
        ClasificacionRiesgoAdapter adapter = new ClasificacionRiesgoAdapter(Optional::empty);

        assertThat(adapter.clasificarConPoliticaVigente(50)).isEmpty();
    }

    @Test
    void debeLeerLaPoliticaVigenteUnaSolaVez() {
        AtomicInteger lecturas = new AtomicInteger();
        PoliticaClasificacionConsultaPort consulta = () -> {
            lecturas.incrementAndGet();
            return Optional.of(politica(7L, 3));
        };

        new ClasificacionRiesgoAdapter(consulta).clasificarConPoliticaVigente(50);

        assertThat(lecturas).hasValue(1);
    }
}
