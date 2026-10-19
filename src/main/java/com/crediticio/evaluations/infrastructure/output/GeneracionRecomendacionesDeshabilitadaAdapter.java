package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort;
import com.crediticio.evaluations.ports.output.ResultadoGeneracion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Saga de recomendaciones deshabilitada (scoring.saga.habilitada=false, valor por defecto): no se llama a
 * evaluation-query-api ni se registra nada; las evaluaciones quedan PENDIENTE hasta que se habilite.
 */
@Component
@ConditionalOnProperty(prefix = "scoring.saga", name = "habilitada", havingValue = "false", matchIfMissing = true)
public class GeneracionRecomendacionesDeshabilitadaAdapter implements GeneracionRecomendacionesPort {

    @Override
    public ResultadoGeneracion generar(long idEvaluacion, ModoIntento modo) {
        return new ResultadoGeneracion.NoDisponible();
    }
}
