package com.crediticio.scoring.domain;

public class ConfiguracionClasificacionNoInicializadaException extends RuntimeException {

    public ConfiguracionClasificacionNoInicializadaException() {
        super("No existe la configuración de la política de clasificación vigente");
    }
}
