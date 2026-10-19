package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de POST /api/v1/evaluaciones con la saga de HU14. Primero confirma la transacción local que
 * registra la evaluación (PENDIENTE) y su trabajo; después, ya sin transacción ni bloqueos, hace el primer
 * intento de generación de recomendaciones. Responde COMPLETADA solo si ese intento quedó registrado; si no,
 * PENDIENTE, y el proceso de reenvío continúa. Si el registro falla, no se hace ninguna llamada.
 */
@Service
@Primary
public class EvaluarSolicitanteService implements CalcularScoreUseCase {

    private final CalcularScoreService calcularScoreService;
    private final ProcesoGeneracionRecomendaciones proceso;
    private final EvaluacionMapper evaluacionMapper;

    public EvaluarSolicitanteService(CalcularScoreService calcularScoreService,
            ProcesoGeneracionRecomendaciones proceso, EvaluacionMapper evaluacionMapper) {
        this.calcularScoreService = calcularScoreService;
        this.proceso = proceso;
        this.evaluacionMapper = evaluacionMapper;
    }

    @Override
    public EvaluacionResponse calcular(CalcularScoreRequest request) {
        EvaluacionRegistrada registrada = calcularScoreService.registrar(request);
        Evaluacion evaluacion = registrada.evaluacion();
        if (proceso.intentar(evaluacion.getIdEvaluacion(), registrada.tokenConcesion(), ModoIntento.EN_LINEA, 1)) {
            evaluacion = evaluacion.completada();
        }
        return evaluacionMapper.aResponse(evaluacion);
    }
}
