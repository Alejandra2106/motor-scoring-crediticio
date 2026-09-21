package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.OperadorScoring;

public class ReglaScoringResponse {

    private final Long idRegla;
    private final Long idRiesgo;
    private final OperadorScoring operador;
    private final String valorCondicion;
    private final Integer puntaje;

    public ReglaScoringResponse(Long idRegla, Long idRiesgo, OperadorScoring operador, String valorCondicion,
            Integer puntaje) {
        this.idRegla = idRegla;
        this.idRiesgo = idRiesgo;
        this.operador = operador;
        this.valorCondicion = valorCondicion;
        this.puntaje = puntaje;
    }

    public Long getIdRegla() {
        return idRegla;
    }

    public Long getIdRiesgo() {
        return idRiesgo;
    }

    public OperadorScoring getOperador() {
        return operador;
    }

    public String getValorCondicion() {
        return valorCondicion;
    }

    public Integer getPuntaje() {
        return puntaje;
    }
}
