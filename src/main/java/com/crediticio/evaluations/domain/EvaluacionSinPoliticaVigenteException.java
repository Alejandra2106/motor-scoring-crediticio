package com.crediticio.evaluations.domain;

/** HU12: no se puede clasificar (ni registrar) una evaluación sin una política de clasificación vigente. */
public class EvaluacionSinPoliticaVigenteException extends RuntimeException {

    public EvaluacionSinPoliticaVigenteException() {
        super("No existe una política de clasificación vigente; la evaluación no se registró");
    }
}
