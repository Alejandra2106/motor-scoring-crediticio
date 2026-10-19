package com.crediticio.evaluations.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Evaluación de score crediticio de un solicitante. El puntaje total (RF07) es la suma de
 * {@code puntajeObtenido} de sus detalles.
 *
 * HU12 CA05: por excepción aprobada a CLAUDE.md §12, el puntaje total se persiste junto con la
 * clasificación y la referencia a la política utilizada. El valor persistido nunca sustituye a la
 * suma: al reconstruir, si no coincide con los detalles la evaluación se considera inconsistente
 * (CA07). Las evaluaciones anteriores a HU12 no tienen puntaje persistido ni clasificación, y no se
 * reclasifican.
 *
 * HU14: toda evaluación nueva nace PENDIENTE y solo pasa a COMPLETADA cuando sus recomendaciones quedan
 * persistidas (regla de la saga). Las evaluaciones anteriores a la saga no tienen estado.
 */
public class Evaluacion {

    private final Long idEvaluacion;
    private final Long idSolicitante;
    private final LocalDateTime fechaEvaluacion;
    private final ClasificacionEvaluacion clasificacion;
    private final EstadoEvaluacion estado;
    private final List<DetalleEvaluacion> detalles;

    private Evaluacion(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            ClasificacionEvaluacion clasificacion, EstadoEvaluacion estado, List<DetalleEvaluacion> detalles) {
        this.idEvaluacion = idEvaluacion;
        this.idSolicitante = Objects.requireNonNull(idSolicitante, "idSolicitante es obligatorio");
        this.fechaEvaluacion = fechaEvaluacion;
        this.clasificacion = clasificacion;
        this.estado = estado;
        this.detalles = List.copyOf(Objects.requireNonNull(detalles, "detalles es obligatorio"));
    }

    /** Toda evaluación nueva debe estar clasificada (HU12 CA03/CA05) y nace PENDIENTE (HU14). */
    public static Evaluacion nueva(Long idSolicitante, List<DetalleEvaluacion> detalles,
            ClasificacionEvaluacion clasificacion) {
        Objects.requireNonNull(clasificacion, "clasificacion es obligatoria");
        return new Evaluacion(null, idSolicitante, null, clasificacion, EstadoEvaluacion.PENDIENTE, detalles);
    }

    /**
     * Evaluación sin estado de saga (registrada antes de HU14).
     *
     * @param scoreTotalPersistido puntaje total almacenado; null solo en evaluaciones anteriores a HU12
     * @param clasificacion        null solo en evaluaciones anteriores a HU12
     */
    public static Evaluacion reconstruir(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            Integer scoreTotalPersistido, ClasificacionEvaluacion clasificacion, List<DetalleEvaluacion> detalles) {
        return reconstruir(idEvaluacion, idSolicitante, fechaEvaluacion, scoreTotalPersistido, clasificacion, null,
                detalles);
    }

    /**
     * @param scoreTotalPersistido puntaje total almacenado; null solo en evaluaciones anteriores a HU12
     * @param clasificacion        null solo en evaluaciones anteriores a HU12
     * @param estado               null solo en evaluaciones anteriores a la saga (HU14)
     */
    public static Evaluacion reconstruir(Long idEvaluacion, Long idSolicitante, LocalDateTime fechaEvaluacion,
            Integer scoreTotalPersistido, ClasificacionEvaluacion clasificacion, EstadoEvaluacion estado,
            List<DetalleEvaluacion> detalles) {
        if ((scoreTotalPersistido == null) != (clasificacion == null)) {
            throw new IllegalStateException("La evaluación " + idEvaluacion
                    + " tiene datos de clasificación incompletos");
        }
        if (estado != null && clasificacion == null) {
            throw new IllegalStateException("La evaluación " + idEvaluacion
                    + " está en la saga sin clasificación persistida");
        }
        Evaluacion evaluacion = new Evaluacion(idEvaluacion, idSolicitante, fechaEvaluacion, clasificacion, estado,
                detalles);
        if (scoreTotalPersistido != null && scoreTotalPersistido != evaluacion.getScoreTotal()) {
            throw new IllegalStateException("El puntaje total almacenado de la evaluación " + idEvaluacion
                    + " no coincide con la suma de sus detalles");
        }
        return evaluacion;
    }

    /** HU14: única transición permitida, PENDIENTE → COMPLETADA, una vez persistidas las recomendaciones. */
    public Evaluacion completada() {
        if (estado != EstadoEvaluacion.PENDIENTE) {
            throw new IllegalStateException("Solo una evaluación PENDIENTE puede completarse (estado actual: "
                    + estado + ")");
        }
        return new Evaluacion(idEvaluacion, idSolicitante, fechaEvaluacion, clasificacion, EstadoEvaluacion.COMPLETADA,
                detalles);
    }

    public static int sumarPuntajes(List<DetalleEvaluacion> detalles) {
        return detalles.stream().mapToInt(DetalleEvaluacion::getPuntajeObtenido).sum();
    }

    public int getScoreTotal() {
        return sumarPuntajes(detalles);
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

    /** Null solo en evaluaciones registradas antes de HU12. */
    public ClasificacionEvaluacion getClasificacion() {
        return clasificacion;
    }

    /** Null solo en evaluaciones registradas antes de la saga (HU14). */
    public EstadoEvaluacion getEstado() {
        return estado;
    }

    public List<DetalleEvaluacion> getDetalles() {
        return detalles;
    }
}
