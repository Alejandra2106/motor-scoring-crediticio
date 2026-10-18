package com.crediticio.scoring.domain;

public class PoliticaVigenteNoDisponibleException extends RuntimeException {

    public PoliticaVigenteNoDisponibleException() {
        super("No existe una política de clasificación vigente");
    }
}
