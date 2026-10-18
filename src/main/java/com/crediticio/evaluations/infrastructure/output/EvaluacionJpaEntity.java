package com.crediticio.evaluations.infrastructure.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * HU12 CA05/CA06: {@code score_total}, {@code categoria} e {@code id_politica} se escriben una
 * sola vez al registrar la evaluación y nunca se actualizan. Son null en evaluaciones anteriores a
 * HU12 (requieren la migración V8, pendiente de aprobación).
 */
@Entity
@Table(name = "evaluacion")
public class EvaluacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    @Column(name = "fecha_evaluacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaEvaluacion;

    @Column(name = "score_total", updatable = false)
    private Integer scoreTotal;

    @Column(name = "categoria", length = 10, updatable = false)
    private String categoria;

    @Column(name = "id_politica", updatable = false)
    private Long idPolitica;

    protected EvaluacionJpaEntity() {
        // requerido por JPA
    }

    public EvaluacionJpaEntity(Long idSolicitante, Integer scoreTotal, String categoria, Long idPolitica) {
        this.idSolicitante = idSolicitante;
        this.scoreTotal = scoreTotal;
        this.categoria = categoria;
        this.idPolitica = idPolitica;
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
}
