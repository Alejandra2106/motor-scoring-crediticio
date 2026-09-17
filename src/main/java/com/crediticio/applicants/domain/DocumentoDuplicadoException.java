package com.crediticio.applicants.domain;

public class DocumentoDuplicadoException extends RuntimeException {

    public DocumentoDuplicadoException() {
        super("El número de documento ya se encuentra registrado");
    }
}
