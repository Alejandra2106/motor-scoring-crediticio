package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.domain.EstadoReglaScoring;
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
@Table(name = "regla_scoring")
public class ReglaScoringJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Long idRegla;

    @Column(name = "id_riesgo", nullable = false)
    private Long idRiesgo;

    @Column(name = "operador", nullable = false, length = 2)
    private String operador;

    @Column(name = "valor_condicion", nullable = false, length = 255)
    private String valorCondicion;

    @Column(name = "puntaje", nullable = false)
    private Integer puntaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoReglaScoring estado;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected ReglaScoringJpaEntity() {
        // requerido por JPA
    }

    public ReglaScoringJpaEntity(Long idRiesgo, String operador, String valorCondicion, Integer puntaje,
            EstadoReglaScoring estado) {
        this.idRiesgo = idRiesgo;
        this.operador = operador;
        this.valorCondicion = valorCondicion;
        this.puntaje = puntaje;
        this.estado = estado;
    }

    public Long getIdRegla() {
        return idRegla;
    }

    public Long getIdRiesgo() {
        return idRiesgo;
    }

    public String getOperador() {
        return operador;
    }

    public String getValorCondicion() {
        return valorCondicion;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public EstadoReglaScoring getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public void setValorCondicion(String valorCondicion) {
        this.valorCondicion = valorCondicion;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }
}
