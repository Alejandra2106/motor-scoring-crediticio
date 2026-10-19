package com.crediticio.riskvariables.infrastructure.output;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "riesgo")
public class VariableRiesgoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_riesgo")
    private Long idRiesgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "variable", nullable = false, unique = true, length = 30)
    private NombreVariableRiesgo variable;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoVariableRiesgo estado;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    protected VariableRiesgoJpaEntity() {
        // requerido por JPA
    }

    public VariableRiesgoJpaEntity(NombreVariableRiesgo variable, String descripcion, EstadoVariableRiesgo estado) {
        this.variable = variable;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Long getIdRiesgo() {
        return idRiesgo;
    }

    public NombreVariableRiesgo getVariable() {
        return variable;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoVariableRiesgo getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setEstado(EstadoVariableRiesgo estado) {
        this.estado = estado;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
