package com.crediticio.riskvariables.application;

import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import org.springframework.stereotype.Component;

@Component
public class VariableRiesgoMapper {

    public VariableRiesgo aDominio(CrearVariableRiesgoRequest request) {
        return VariableRiesgo.nueva(request.getVariable(), request.getDescripcion());
    }

    public VariableRiesgoResponse aResponse(VariableRiesgo variableRiesgo) {
        return new VariableRiesgoResponse(
                variableRiesgo.getIdVariableRiesgo(),
                variableRiesgo.getVariable(),
                variableRiesgo.getTipo(),
                variableRiesgo.getDescripcion(),
                variableRiesgo.getEstado());
    }
}
