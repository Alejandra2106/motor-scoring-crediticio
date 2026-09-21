package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.DetalleEvaluacionResponse;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import org.springframework.stereotype.Component;

@Component
public class EvaluacionMapper {

    public EvaluacionResponse aResponse(Evaluacion evaluacion) {
        return new EvaluacionResponse(
                evaluacion.getIdEvaluacion(),
                evaluacion.getIdSolicitante(),
                evaluacion.getFechaEvaluacion(),
                evaluacion.getScoreTotal(),
                evaluacion.getDetalles().stream().map(this::aDetalleResponse).toList());
    }

    private DetalleEvaluacionResponse aDetalleResponse(DetalleEvaluacion detalle) {
        return new DetalleEvaluacionResponse(
                detalle.getIdRegla(),
                detalle.getOperadorAplicado(),
                detalle.getValorCondicionAplicado(),
                detalle.isCondicionCumplida(),
                detalle.getPuntajeObtenido());
    }
}
