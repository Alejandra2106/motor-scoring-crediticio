package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.EvaluacionNoEncontradaException;
import com.crediticio.evaluations.ports.input.ConsultarEvaluacionUseCase;
import com.crediticio.evaluations.ports.output.EvaluacionConsultaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU12 CA09/CA10: recupera una evaluación con su puntaje, clasificación y versión de política tal
 * como se registraron, sin reclasificarla con la política vigente (CA06).
 */
@Service
public class ConsultarEvaluacionService implements ConsultarEvaluacionUseCase {

    private final EvaluacionConsultaPort evaluacionConsultaPort;
    private final EvaluacionMapper evaluacionMapper;

    public ConsultarEvaluacionService(EvaluacionConsultaPort evaluacionConsultaPort,
            EvaluacionMapper evaluacionMapper) {
        this.evaluacionConsultaPort = evaluacionConsultaPort;
        this.evaluacionMapper = evaluacionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluacionResponse consultarPorId(Long idEvaluacion) {
        return evaluacionConsultaPort.buscarPorId(idEvaluacion)
                .map(evaluacionMapper::aResponse)
                .orElseThrow(EvaluacionNoEncontradaException::new);
    }
}
