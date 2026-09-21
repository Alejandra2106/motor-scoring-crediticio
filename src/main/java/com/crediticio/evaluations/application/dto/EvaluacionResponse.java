package com.crediticio.evaluations.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EvaluacionResponse {

    private final Long idEvaluacion;
    private final Long idSolicitante;
    private final LocalDateTime fechaEvaluacion;
    private final Integer scoreTotal;
    private final List<DetalleEvaluacionResponse> detalles;

    public EvaluacionResponse(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            Integer scoreTotal, List<DetalleEvaluacionResponse> detalles) {
        this.idEvaluacion = idEvaluacion;
        this.idSolicitante = idSolicitante;
        this.fechaEvaluacion = fechaEvaluacion;
        this.scoreTotal = scoreTotal;
        this.detalles = detalles;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public Long getIdSolicitante() {
        return idSolicitante;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public Integer getScoreTotal() {
        return scoreTotal;
    }

    public List<DetalleEvaluacionResponse> getDetalles() {
        return detalles;
    }
}
