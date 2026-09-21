package com.crediticio.scoring.ports.input;

import com.crediticio.scoring.application.dto.CrearReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;

public interface CrearReglaScoringUseCase {

    ReglaScoringResponse crear(CrearReglaScoringRequest request);
}
