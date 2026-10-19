package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.domain.CategoriaRiesgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Igual que {@link PoliticaClasificacionJpaEntity}: columnas {@code updatable = false} y sin
 * setters, para que un rango publicado nunca genere UPDATE desde Hibernate.
 */
@Entity
@Table(name = "rango_clasificacion")
public class RangoClasificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rango")
    private Long idRango;

    @Column(name = "id_politica", nullable = false, updatable = false)
    private Long idPolitica;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 10, updatable = false)
    private CategoriaRiesgo categoria;

    @Column(name = "limite_inferior", nullable = false, updatable = false)
    private Integer limiteInferior;

    @Column(name = "limite_superior", nullable = false, updatable = false)
    private Integer limiteSuperior;

    protected RangoClasificacionJpaEntity() {
        // requerido por JPA
    }

    public RangoClasificacionJpaEntity(Long idPolitica, CategoriaRiesgo categoria, Integer limiteInferior,
            Integer limiteSuperior) {
        this.idPolitica = idPolitica;
        this.categoria = categoria;
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
    }

    public Long getIdRango() {
        return idRango;
    }

    public Long getIdPolitica() {
        return idPolitica;
    }

    public CategoriaRiesgo getCategoria() {
        return categoria;
    }

    public Integer getLimiteInferior() {
        return limiteInferior;
    }

    public Integer getLimiteSuperior() {
        return limiteSuperior;
    }
}
