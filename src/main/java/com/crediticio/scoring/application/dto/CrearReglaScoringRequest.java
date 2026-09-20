package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.OperadorScoring;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public class CrearReglaScoringRequest {

    @NotNull(message = "idRiesgo es obligatorio")
    private Long idRiesgo;

    @NotNull(message = "operador es obligatorio")
    private OperadorScoring operador;

    @NotBlank(message = "valorCondicion es obligatorio")
    private String valorCondicion;

    @NotNull(message = "puntaje es obligatorio")
    @Min(value = -100, message = "puntaje debe ser mayor o igual a -100")
    @Max(value = 100, message = "puntaje debe ser menor o igual a 100")
    @JsonDeserialize(using = PuntajeReglaScoringDeserializer.class)
    private Integer puntaje;

    public CrearReglaScoringRequest() {
    }

    public CrearReglaScoringRequest(Long idRiesgo, OperadorScoring operador, String valorCondicion, Integer puntaje) {
        this.idRiesgo = idRiesgo;
        this.operador = operador;
        this.valorCondicion = valorCondicion;
        this.puntaje = puntaje;
    }

    public Long getIdRiesgo() {
        return idRiesgo;
    }

    public void setIdRiesgo(Long idRiesgo) {
        this.idRiesgo = idRiesgo;
    }

    public OperadorScoring getOperador() {
        return operador;
    }

    public void setOperador(OperadorScoring operador) {
        this.operador = operador;
    }

    public String getValorCondicion() {
        return valorCondicion;
    }

    public void setValorCondicion(String valorCondicion) {
        this.valorCondicion = valorCondicion;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }
}
