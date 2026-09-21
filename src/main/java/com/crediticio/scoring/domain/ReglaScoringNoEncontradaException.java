package com.crediticio.scoring.domain;

public class ReglaScoringNoEncontradaException extends RuntimeException {

    public ReglaScoringNoEncontradaException() {
        super("No se encontró una regla de scoring con el idRegla indicado");
    }
}
