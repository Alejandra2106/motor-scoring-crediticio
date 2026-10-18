package com.crediticio.evaluations.domain;

import java.util.Objects;

/**
 * Clasificación de riesgo asignada a una evaluación (HU12): la categoría obtenida y la referencia a
 * la versión exacta de la política de clasificación con la que se obtuvo (CA05). Una vez asignada no
 * se recalcula aunque cambie la política vigente (CA06). La categoría describe el riesgo; no
 * equivale a una aprobación o un rechazo del crédito (CA12).
 */
public record ClasificacionEvaluacion(String categoria, Long idPolitica, Integer versionPolitica) {

    public ClasificacionEvaluacion {
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("categoria es obligatoria");
        }
        Objects.requireNonNull(idPolitica, "idPolitica es obligatorio");
        Objects.requireNonNull(versionPolitica, "versionPolitica es obligatoria");
    }
}
