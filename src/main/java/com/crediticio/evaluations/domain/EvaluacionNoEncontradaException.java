package com.crediticio.evaluations.domain;

/** HU12 CA10: la evaluación consultada no existe. */
public class EvaluacionNoEncontradaException extends RuntimeException {

    public EvaluacionNoEncontradaException() {
        super("No se encontró una evaluación con el identificador indicado");
    }
}
