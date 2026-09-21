package com.crediticio.evaluations.infrastructure.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

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

    protected EvaluacionJpaEntity() {
        // requerido por JPA
    }

    public EvaluacionJpaEntity(Long idSolicitante) {
        this.idSolicitante = idSolicitante;
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
}
