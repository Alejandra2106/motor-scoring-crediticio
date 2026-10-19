package com.crediticio.evaluations.domain;

/**
 * Identificador de una variable de riesgo, en la representación propia de {@code evaluations}.
 * La fuente de verdad de esta clasificación sigue siendo el módulo {@code riskvariables};
 * este enum existe para que el dominio de {@code evaluations} no dependa de las clases de
 * dominio de ese módulo.
 */
public enum NombreVariableEvaluada {
    INGRESOS_MENSUALES,
    NIVEL_ENDEUDAMIENTO,
    NUMERO_MORAS,
    HISTORIAL_CREDITICIO,
    ANTIGUEDAD_LABORAL
}
