package com.crediticio.scoring.ports.input;

import com.crediticio.scoring.application.dto.EditarReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;

public interface EditarReglaScoringUseCase {

    ReglaScoringResponse editar(Long idRegla, EditarReglaScoringRequest request);
}
