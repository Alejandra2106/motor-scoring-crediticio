package com.crediticio.scoring.domain;

public class ReglaScoringVariableNoEncontradaException extends RuntimeException {

    public ReglaScoringVariableNoEncontradaException() {
        super("No se encontró una variable de riesgo con el idRiesgo indicado");
    }
}
