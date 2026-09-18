package com.crediticio.riskvariables.application.dto;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.TipoVariableRiesgo;

public class VariableRiesgoResponse {

    private final Long idVariableRiesgo;
    private final NombreVariableRiesgo variable;
    private final TipoVariableRiesgo tipo;
    private final String descripcion;
    private final EstadoVariableRiesgo estado;

    public VariableRiesgoResponse(Long idVariableRiesgo, NombreVariableRiesgo variable, TipoVariableRiesgo tipo,
            String descripcion, EstadoVariableRiesgo estado) {
        this.idVariableRiesgo = idVariableRiesgo;
        this.variable = variable;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Long getIdVariableRiesgo() {
        return idVariableRiesgo;
    }

    public NombreVariableRiesgo getVariable() {
        return variable;
    }

    public TipoVariableRiesgo getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoVariableRiesgo getEstado() {
        return estado;
    }
}
