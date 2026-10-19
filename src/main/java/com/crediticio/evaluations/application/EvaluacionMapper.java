package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.DetalleEvaluacionResponse;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.ClasificacionEvaluacion;
import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import org.springframework.stereotype.Component;

@Component
public class EvaluacionMapper {

    public EvaluacionResponse aResponse(Evaluacion evaluacion) {
        ClasificacionEvaluacion clasificacion = evaluacion.getClasificacion();
        return new EvaluacionResponse(
                evaluacion.getIdEvaluacion(),
                evaluacion.getIdSolicitante(),
                evaluacion.getFechaEvaluacion(),
                evaluacion.getScoreTotal(),
                clasificacion == null ? null : clasificacion.categoria(),
                clasificacion == null ? null : clasificacion.idPolitica(),
                clasificacion == null ? null : clasificacion.versionPolitica(),
                evaluacion.getEstado() == null ? null : evaluacion.getEstado().name(),
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
