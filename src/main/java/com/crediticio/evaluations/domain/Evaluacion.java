package com.crediticio.evaluations.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Evaluación de score crediticio de un solicitante. El puntaje total (RF07/CLAUDE.md §12) se
 * calcula en tiempo de uso como la suma de {@code puntajeObtenido} de sus detalles; nunca se
 * persiste como columna propia.
 */
public class Evaluacion {

    private final Long idEvaluacion;
    private final Long idSolicitante;
    private final LocalDateTime fechaEvaluacion;
    private final List<DetalleEvaluacion> detalles;

    private Evaluacion(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            List<DetalleEvaluacion> detalles) {
        this.idEvaluacion = idEvaluacion;
        this.idSolicitante = Objects.requireNonNull(idSolicitante, "idSolicitante es obligatorio");
        this.fechaEvaluacion = fechaEvaluacion;
        this.detalles = List.copyOf(Objects.requireNonNull(detalles, "detalles es obligatorio"));
    }

    public static Evaluacion nueva(Long idSolicitante, List<DetalleEvaluacion> detalles) {
        return new Evaluacion(null, idSolicitante, null, detalles);
    }

    public static Evaluacion reconstruir(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            List<DetalleEvaluacion> detalles) {
        return new Evaluacion(idEvaluacion, idSolicitante, fechaEvaluacion, detalles);
    }

    public int getScoreTotal() {
        return detalles.stream().mapToInt(DetalleEvaluacion::getPuntajeObtenido).sum();
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

    public List<DetalleEvaluacion> getDetalles() {
        return detalles;
    }
}
