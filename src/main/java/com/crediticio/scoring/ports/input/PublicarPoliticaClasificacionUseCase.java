package com.crediticio.scoring.ports.input;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;

public interface PublicarPoliticaClasificacionUseCase {

    PoliticaClasificacionResponse publicar(PublicarPoliticaClasificacionRequest request);
}
