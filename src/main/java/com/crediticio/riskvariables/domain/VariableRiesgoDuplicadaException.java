package com.crediticio.riskvariables.domain;

public class VariableRiesgoDuplicadaException extends RuntimeException {

    public VariableRiesgoDuplicadaException() {
        super("La variable de riesgo ya se encuentra registrada");
    }
}
