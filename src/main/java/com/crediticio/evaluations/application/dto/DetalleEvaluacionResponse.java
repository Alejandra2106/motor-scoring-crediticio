package com.crediticio.evaluations.application.dto;

public class DetalleEvaluacionResponse {

    private final Long idRegla;
    private final String operadorAplicado;
    private final String valorCondicionAplicado;
    private final Boolean condicionCumplida;
    private final Integer puntajeObtenido;

    public DetalleEvaluacionResponse(Long idRegla, String operadorAplicado, String valorCondicionAplicado,
            Boolean condicionCumplida, Integer puntajeObtenido) {
        this.idRegla = idRegla;
        this.operadorAplicado = operadorAplicado;
        this.valorCondicionAplicado = valorCondicionAplicado;
        this.condicionCumplida = condicionCumplida;
        this.puntajeObtenido = puntajeObtenido;
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

    public Boolean getCondicionCumplida() {
        return condicionCumplida;
    }

    public Integer getPuntajeObtenido() {
        return puntajeObtenido;
    }
}
