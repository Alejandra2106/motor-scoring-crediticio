package com.crediticio.scoring.domain;

import java.util.List;

public class PoliticaClasificacionInvalidaException extends RuntimeException {

    private final List<String> errores;

    public PoliticaClasificacionInvalidaException(List<String> errores) {
        super("La política de clasificación no es válida");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
