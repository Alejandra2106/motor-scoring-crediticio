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
}
