package com.crediticio.scoring.ports.input;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;

public interface ConsultarPoliticaVigenteUseCase {

    PoliticaClasificacionResponse consultarVigente();
}
