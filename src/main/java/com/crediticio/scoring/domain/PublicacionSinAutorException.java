package com.crediticio.scoring.domain;

/** CA14: una política solo se publica con un autor autenticado; nunca con un autor inventado. */
public class PublicacionSinAutorException extends RuntimeException {

    public PublicacionSinAutorException() {
        super("La publicación de una política de clasificación requiere un usuario autenticado");
    }
}
