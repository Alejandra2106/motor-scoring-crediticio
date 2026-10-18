package com.crediticio.evaluations.ports.output;

import com.crediticio.evaluations.domain.Evaluacion;

import java.util.Optional;

/** Consulta de solo lectura, separada del puerto de registro (HU12 CA09). */
public interface EvaluacionConsultaPort {

    Optional<Evaluacion> buscarPorId(Long idEvaluacion);
}
