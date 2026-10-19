package com.crediticio.evaluations.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU12: además de los campos de HU07 incluye la categoría de riesgo y la referencia a la versión
 * exacta de la política utilizada; son null en evaluaciones registradas antes de HU12. La categoría
 * no representa una aprobación ni un rechazo del crédito (CA12).
 *
 * HU14: estado de la saga (PENDIENTE o COMPLETADA), añadido de forma aditiva; null en evaluaciones
 * registradas antes de la saga. Una evaluación PENDIENTE no debe presentarse como completa.
 */
public class EvaluacionResponse {

    private final Long idEvaluacion;
    private final Long idSolicitante;
    private final LocalDateTime fechaEvaluacion;
    private final Integer scoreTotal;
    private final String categoria;
    private final Long idPolitica;
    private final Integer versionPolitica;
    private final String estado;
    private final List<DetalleEvaluacionResponse> detalles;

    public EvaluacionResponse(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            Integer scoreTotal, String categoria, Long idPolitica, Integer versionPolitica,
            List<DetalleEvaluacionResponse> detalles) {
        this(idEvaluacion, idSolicitante, fechaEvaluacion, scoreTotal, categoria, idPolitica, versionPolitica, null,
                detalles);
    }

    public EvaluacionResponse(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            Integer scoreTotal, String categoria, Long idPolitica, Integer versionPolitica, String estado,
            List<DetalleEvaluacionResponse> detalles) {
        this.idEvaluacion = idEvaluacion;
        this.idSolicitante = idSolicitante;
        this.fechaEvaluacion = fechaEvaluacion;
        this.scoreTotal = scoreTotal;
        this.categoria = categoria;
        this.idPolitica = idPolitica;
        this.versionPolitica = versionPolitica;
        this.estado = estado;
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

    /** PENDIENTE o COMPLETADA; null en evaluaciones anteriores a la saga (HU14). */
    public String getEstado() {
        return estado;
    }

    public List<DetalleEvaluacionResponse> getDetalles() {
        return detalles;
    }
}
