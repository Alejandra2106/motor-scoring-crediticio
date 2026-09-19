package com.crediticio.riskvariables.application.dto;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;

public class CambiarEstadoVariableRiesgoResponse {

    private final Long idRiesgo;
    private final EstadoVariableRiesgo estadoAnterior;
    private final EstadoVariableRiesgo estadoNuevo;

    public CambiarEstadoVariableRiesgoResponse(Long idRiesgo, EstadoVariableRiesgo estadoAnterior,
            EstadoVariableRiesgo estadoNuevo) {
        this.idRiesgo = idRiesgo;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
    }

    public Long getIdRiesgo() {
        return idRiesgo;
    }

    public EstadoVariableRiesgo getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoVariableRiesgo getEstadoNuevo() {
        return estadoNuevo;
    }
}
