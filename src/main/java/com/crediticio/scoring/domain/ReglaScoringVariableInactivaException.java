package com.crediticio.scoring.domain;

public class ReglaScoringVariableInactivaException extends RuntimeException {

    public ReglaScoringVariableInactivaException() {
        super("La variable de riesgo asociada se encuentra INACTIVA");
    }
}
