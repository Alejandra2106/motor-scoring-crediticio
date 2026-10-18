package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.shared.util.EnteroEstrictoDeserializer;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public class RangoClasificacionRequest {

    @NotNull(message = "categoria es obligatoria")
    private CategoriaRiesgo categoria;

    // HU11 CA04: los límites deben ser enteros; un decimal (p. ej. 10.7) se rechaza en vez de truncarse
    @NotNull(message = "limiteInferior es obligatorio")
    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    private Integer limiteInferior;

    @NotNull(message = "limiteSuperior es obligatorio")
    @JsonDeserialize(using = EnteroEstrictoDeserializer.class)
    private Integer limiteSuperior;

    public RangoClasificacionRequest() {
    }

    public RangoClasificacionRequest(CategoriaRiesgo categoria, Integer limiteInferior, Integer limiteSuperior) {
        this.categoria = categoria;
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
    }

    public CategoriaRiesgo getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaRiesgo categoria) {
        this.categoria = categoria;
    }

    public Integer getLimiteInferior() {
        return limiteInferior;
    }

    public void setLimiteInferior(Integer limiteInferior) {
        this.limiteInferior = limiteInferior;
    }

    public Integer getLimiteSuperior() {
        return limiteSuperior;
    }

    public void setLimiteSuperior(Integer limiteSuperior) {
        this.limiteSuperior = limiteSuperior;
    }
}
