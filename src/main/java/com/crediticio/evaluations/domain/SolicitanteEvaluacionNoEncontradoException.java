package com.crediticio.evaluations.domain;

public class SolicitanteEvaluacionNoEncontradoException extends RuntimeException {

    public SolicitanteEvaluacionNoEncontradoException() {
        super("No se encontró un solicitante con el identificador indicado");
    }
}
