package com.crediticio.evaluations.ports.output;

import com.crediticio.evaluations.domain.Evaluacion;

public interface EvaluacionRepositoryPort {

    Evaluacion guardar(Evaluacion evaluacion);
}
