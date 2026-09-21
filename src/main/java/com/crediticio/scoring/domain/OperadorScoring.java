package com.crediticio.scoring.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OperadorScoring {

    IGUAL("="),
    MAYOR(">"),
    MAYOR_O_IGUAL(">="),
    MENOR("<"),
    MENOR_O_IGUAL("<=");

    private final String simbolo;

    OperadorScoring(String simbolo) {
        this.simbolo = simbolo;
    }

    @JsonValue
    public String getSimbolo() {
        return simbolo;
    }

    @JsonCreator
    public static OperadorScoring desdeSimbolo(String simbolo) {
        for (OperadorScoring operador : values()) {
            if (operador.simbolo.equals(simbolo)) {
                return operador;
            }
        }
        throw new IllegalArgumentException("operador debe ser uno de: =, >, >=, <, <=");
    }

    public boolean esSoloIgualdad() {
        return this == IGUAL;
    }

    /**
     * Interpreta el resultado de un {@code compareTo} entre el valor real y el valor de
     * condición según la semántica de este operador (p. ej. para {@code MAYOR}, cumple
     * cuando el valor real es mayor que la condición).
     */
    public boolean comparar(int resultadoComparacion) {
        return switch (this) {
            case IGUAL -> resultadoComparacion == 0;
            case MAYOR -> resultadoComparacion > 0;
            case MAYOR_O_IGUAL -> resultadoComparacion >= 0;
            case MENOR -> resultadoComparacion < 0;
            case MENOR_O_IGUAL -> resultadoComparacion <= 0;
        };
    }
}
