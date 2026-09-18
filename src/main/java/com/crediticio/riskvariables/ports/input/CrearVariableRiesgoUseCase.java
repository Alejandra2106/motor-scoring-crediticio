package com.crediticio.riskvariables.ports.input;

import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;

public interface CrearVariableRiesgoUseCase {

    VariableRiesgoResponse crear(CrearVariableRiesgoRequest request);
}
