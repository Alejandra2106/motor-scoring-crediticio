package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de POST /api/v1/evaluaciones con la saga de HU14. Primero confirma la transacción local que
 * registra la evaluación (PENDIENTE) y su trabajo; después, ya sin transacción ni bloqueos, hace el primer
 * intento de generación de recomendaciones. Responde COMPLETADA solo si ese intento quedó registrado; si no,
 * PENDIENTE, y el proceso de reenvío continúa. Si el registro falla, no se hace ninguna llamada. Un fallo
 * del intento en línea (por ejemplo, la base no responde al registrar su resultado) no convierte en error una
 * evaluación ya confirmada: se responde PENDIENTE, para que el cliente no la repita y la duplique.
 */
@Service
@Primary
public class EvaluarSolicitanteService implements CalcularScoreUseCase {

    private static final Logger log = LoggerFactory.getLogger(EvaluarSolicitanteService.class);

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
        if (intentarEnLinea(evaluacion.getIdEvaluacion(), registrada)) {
            evaluacion = evaluacion.completada();
        }
        return evaluacionMapper.aResponse(evaluacion);
    }

    private boolean intentarEnLinea(long idEvaluacion, EvaluacionRegistrada registrada) {
        try {
            return proceso.intentar(idEvaluacion, registrada.tokenConcesion(), ModoIntento.EN_LINEA, 1);
        } catch (RuntimeException ex) {
            // la concesión vence sola y el proceso de reenvío retoma el trabajo
            log.warn("Evaluación {}: falló el intento en línea de la saga ({}); queda PENDIENTE", idEvaluacion,
                    ex.getClass().getSimpleName());
            return false;
        }
    }
}
