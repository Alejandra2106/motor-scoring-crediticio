package com.crediticio.riskvariables.domain;

public class VariableRiesgoNoEncontradaException extends RuntimeException {

    public VariableRiesgoNoEncontradaException() {
        super("No se encontró una variable de riesgo con el identificador indicado");
    }
}
