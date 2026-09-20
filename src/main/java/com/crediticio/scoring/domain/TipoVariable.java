package com.crediticio.scoring.domain;

/**
 * Clasificación de una variable de riesgo, en la representación propia de {@code scoring}.
 * La fuente de verdad de esta clasificación sigue siendo el módulo {@code riskvariables};
 * este tipo existe para que el dominio de {@code scoring} no dependa de las clases de
 * dominio de ese módulo.
 */
public enum TipoVariable {
    NUMERICO,
    CATEGORICO
}
