package com.crediticio.scoring.domain;

public class ReglaScoringDuplicadaException extends RuntimeException {

    public ReglaScoringDuplicadaException() {
        super("Ya existe una regla de scoring con la misma variable, operador y valor de condición");
    }
}
