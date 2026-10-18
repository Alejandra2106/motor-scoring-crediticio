package com.crediticio.evaluations.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU12: además de los campos de HU07 incluye la categoría de riesgo y la referencia a la versión
 * exacta de la política utilizada; son null en evaluaciones registradas antes de HU12. La categoría
 * no representa una aprobación ni un rechazo del crédito (CA12).
 */
public class EvaluacionResponse {

    private final Long idEvaluacion;
    private final Long idSolicitante;
    private final LocalDateTime fechaEvaluacion;
    private final Integer scoreTotal;
    private final String categoria;
    private final Long idPolitica;
    private final Integer versionPolitica;
    private final List<DetalleEvaluacionResponse> detalles;

    public EvaluacionResponse(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            Integer scoreTotal, String categoria, Long idPolitica, Integer versionPolitica,
            List<DetalleEvaluacionResponse> detalles) {
        this.idEvaluacion = idEvaluacion;
        this.idSolicitante = idSolicitante;
        this.fechaEvaluacion = fechaEvaluacion;
        this.scoreTotal = scoreTotal;
        this.categoria = categoria;
        this.idPolitica = idPolitica;
        this.versionPolitica = versionPolitica;
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

    public String getCategoria() {
        return categoria;
    }

    public Long getIdPolitica() {
        return idPolitica;
    }

    public Integer getVersionPolitica() {
        return versionPolitica;
    }

    public List<DetalleEvaluacionResponse> getDetalles() {
        return detalles;
    }
}
