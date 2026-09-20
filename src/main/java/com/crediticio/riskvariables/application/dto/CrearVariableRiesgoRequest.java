package com.crediticio.riskvariables.application.dto;

import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CrearVariableRiesgoRequest {

    @Schema(description = "Variable de riesgo que se desea registrar. Corresponde a un catálogo cerrado de valores.",
            example = "INGRESOS_MENSUALES",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"INGRESOS_MENSUALES", "NIVEL_ENDEUDAMIENTO", "NUMERO_MORAS", "HISTORIAL_CREDITICIO",
                    "ANTIGUEDAD_LABORAL"})
    @NotNull(message = "variable es obligatoria")
    private NombreVariableRiesgo variable;

    @NotBlank(message = "descripcion es obligatoria")
    @Size(min = 10, max = 255, message = "descripcion debe tener entre 10 y 255 caracteres")
    private String descripcion;

    public CrearVariableRiesgoRequest() {
    }

    public CrearVariableRiesgoRequest(NombreVariableRiesgo variable, String descripcion) {
        this.variable = variable;
        this.descripcion = descripcion;
    }

    public NombreVariableRiesgo getVariable() {
        return variable;
    }

    public void setVariable(NombreVariableRiesgo variable) {
        this.variable = variable;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
