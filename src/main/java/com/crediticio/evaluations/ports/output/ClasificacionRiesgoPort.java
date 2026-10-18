package com.crediticio.evaluations.ports.output;

import java.util.Optional;

public interface ClasificacionRiesgoPort {

    /**
     * Lee una sola vez la política de clasificación vigente y clasifica el puntaje con esa misma
     * versión (HU12 CA02). Retorna vacío si no hay política vigente; si la hay pero el puntaje no
     * pertenece a ninguno de sus rangos, el resultado no tiene categoría (CA04).
     */
    Optional<ResultadoClasificacion> clasificarConPoliticaVigente(int puntajeTotal);
}
