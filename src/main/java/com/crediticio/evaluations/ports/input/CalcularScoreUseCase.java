package com.crediticio.evaluations.ports.input;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;

public interface CalcularScoreUseCase {

    EvaluacionResponse calcular(CalcularScoreRequest request);
}
