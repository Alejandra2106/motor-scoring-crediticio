package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.CategoriaRiesgo;

public class RangoClasificacionResponse {

    private final Long idRango;
    private final CategoriaRiesgo categoria;
    private final Integer limiteInferior;
    private final Integer limiteSuperior;

    public RangoClasificacionResponse(Long idRango, CategoriaRiesgo categoria, Integer limiteInferior,
            Integer limiteSuperior) {
        this.idRango = idRango;
        this.categoria = categoria;
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
    }

    public Long getIdRango() {
        return idRango;
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
