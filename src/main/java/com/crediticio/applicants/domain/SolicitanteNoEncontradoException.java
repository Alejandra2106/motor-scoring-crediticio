package com.crediticio.applicants.domain;

public class SolicitanteNoEncontradoException extends RuntimeException {

    public SolicitanteNoEncontradoException() {
        super("No se encontró un solicitante con el número de documento indicado");
    }
}
