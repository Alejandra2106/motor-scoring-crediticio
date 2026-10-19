package com.crediticio.evaluations.domain;

/**
 * Estado de la saga de recomendaciones (HU14). PENDIENTE: la operación no ha terminado y la evaluación no debe
 * presentarse como completa. COMPLETADA: puntaje, clasificación, detalles y recomendaciones persistidos. Las
 * evaluaciones anteriores a la saga no tienen estado (null) y no se incorporan a ella.
 */
public enum EstadoEvaluacion {
    PENDIENTE,
    COMPLETADA
}
