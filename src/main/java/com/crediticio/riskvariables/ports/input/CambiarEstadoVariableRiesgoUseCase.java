package com.crediticio.riskvariables.ports.input;

import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoResponse;

public interface CambiarEstadoVariableRiesgoUseCase {

    CambiarEstadoVariableRiesgoResponse cambiarEstado(Long idRiesgo, CambiarEstadoVariableRiesgoRequest request);
}
