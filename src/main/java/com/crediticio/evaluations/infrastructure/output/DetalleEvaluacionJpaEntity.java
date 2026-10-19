package com.crediticio.evaluations.infrastructure.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "detalle_evaluacion")
public class DetalleEvaluacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_evaluacion")
    private Long idDetalleEvaluacion;

    @Column(name = "id_evaluacion", nullable = false)
    private Long idEvaluacion;

    @Column(name = "id_regla", nullable = false)
    private Long idRegla;

    @Column(name = "operador_aplicado", nullable = false, length = 2)
    private String operadorAplicado;

    @Column(name = "valor_condicion_aplicado", nullable = false, length = 255)
    private String valorCondicionAplicado;

    @Column(name = "puntaje_regla_aplicado", nullable = false)
    private Integer puntajeReglaAplicado;

    @Column(name = "condicion_cumplida", nullable = false)
    private Boolean condicionCumplida;

    @Column(name = "puntaje_obtenido", nullable = false)
    private Integer puntajeObtenido;

    protected DetalleEvaluacionJpaEntity() {
        // requerido por JPA
    }

    public DetalleEvaluacionJpaEntity(Long idEvaluacion, Long idRegla, String operadorAplicado,
            String valorCondicionAplicado, Integer puntajeReglaAplicado, Boolean condicionCumplida,
            Integer puntajeObtenido) {
        this.idEvaluacion = idEvaluacion;
        this.idRegla = idRegla;
        this.operadorAplicado = operadorAplicado;
        this.valorCondicionAplicado = valorCondicionAplicado;
        this.puntajeReglaAplicado = puntajeReglaAplicado;
        this.condicionCumplida = condicionCumplida;
        this.puntajeObtenido = puntajeObtenido;
    }

    public Long getIdDetalleEvaluacion() {
        return idDetalleEvaluacion;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public Long getIdRegla() {
        return idRegla;
    }

    public String getOperadorAplicado() {
        return operadorAplicado;
    }

    public String getValorCondicionAplicado() {
        return valorCondicionAplicado;
    }

    public Integer getPuntajeReglaAplicado() {
        return puntajeReglaAplicado;
    }

    public Boolean getCondicionCumplida() {
        return condicionCumplida;
    }

    public Integer getPuntajeObtenido() {
        return puntajeObtenido;
    }
}
