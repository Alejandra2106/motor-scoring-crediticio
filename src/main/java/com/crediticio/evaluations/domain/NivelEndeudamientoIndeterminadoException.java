package com.crediticio.evaluations.domain;

public class NivelEndeudamientoIndeterminadoException extends RuntimeException {

    public NivelEndeudamientoIndeterminadoException() {
        super("No es posible determinar NIVEL_ENDEUDAMIENTO porque los ingresos mensuales del solicitante son cero");
    }
}
