package com.crediticio.riskvariables.application.dto;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import jakarta.validation.constraints.NotNull;

public class CambiarEstadoVariableRiesgoRequest {

    @NotNull(message = "estado es obligatorio")
    private EstadoVariableRiesgo estado;

    public CambiarEstadoVariableRiesgoRequest() {
    }

    public CambiarEstadoVariableRiesgoRequest(EstadoVariableRiesgo estado) {
        this.estado = estado;
    }

    public EstadoVariableRiesgo getEstado() {
        return estado;
    }

    public void setEstado(EstadoVariableRiesgo estado) {
        this.estado = estado;
    }
}
