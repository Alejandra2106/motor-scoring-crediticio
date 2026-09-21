package com.crediticio.evaluations.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

public class CalcularScoreRequest {

    private Long idSolicitante;

    @Pattern(regexp = "\\d{6,15}", message = "numeroDocumento debe contener entre 6 y 15 dígitos")
    private String numeroDocumento;

    public CalcularScoreRequest() {
    }

    public CalcularScoreRequest(Long idSolicitante, String numeroDocumento) {
        this.idSolicitante = idSolicitante;
        this.numeroDocumento = numeroDocumento;
    }

    @AssertTrue(message = "Debe proporcionar exactamente uno de idSolicitante o numeroDocumento")
    private boolean isIdentificacionValida() {
        return (idSolicitante != null) ^ (numeroDocumento != null);
    }

    public Long getIdSolicitante() {
        return idSolicitante;
    }

    public void setIdSolicitante(Long idSolicitante) {
        this.idSolicitante = idSolicitante;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }
}
