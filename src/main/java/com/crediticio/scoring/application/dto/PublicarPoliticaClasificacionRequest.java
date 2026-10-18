package com.crediticio.scoring.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PublicarPoliticaClasificacionRequest {

    @NotNull(message = "rangos es obligatorio")
    private List<@Valid RangoClasificacionRequest> rangos;

    public PublicarPoliticaClasificacionRequest() {
    }

    public PublicarPoliticaClasificacionRequest(List<RangoClasificacionRequest> rangos) {
        this.rangos = rangos;
    }

    public List<RangoClasificacionRequest> getRangos() {
        return rangos;
    }

    public void setRangos(List<RangoClasificacionRequest> rangos) {
        this.rangos = rangos;
    }
}
