package com.crediticio.scoring.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PoliticaClasificacionResponse {

    private final Long idPolitica;
    private final Integer version;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaPublicacion;
    private final List<RangoClasificacionResponse> rangos;

    public PoliticaClasificacionResponse(Long idPolitica, Integer version, LocalDateTime fechaCreacion,
            LocalDateTime fechaPublicacion, List<RangoClasificacionResponse> rangos) {
        this.idPolitica = idPolitica;
        this.version = version;
        this.fechaCreacion = fechaCreacion;
        this.fechaPublicacion = fechaPublicacion;
        this.rangos = List.copyOf(rangos);
    }

    public Long getIdPolitica() {
        return idPolitica;
    }

    public Integer getVersion() {
        return version;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public List<RangoClasificacionResponse> getRangos() {
        return rangos;
    }
}
