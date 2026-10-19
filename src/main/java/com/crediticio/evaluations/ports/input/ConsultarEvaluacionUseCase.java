package com.crediticio.evaluations.ports.input;

import com.crediticio.evaluations.application.dto.EvaluacionResponse;

public interface ConsultarEvaluacionUseCase {

    EvaluacionResponse consultarPorId(Long idEvaluacion);
}
