package com.crediticio.applicants.application.dto;

import java.time.LocalDateTime;

public class SolicitanteResponse {

    private final Long idSolicitante;
    private final String nombreCompleto;
    private final String numeroDocumento;
    private final LocalDateTime fechaRegistro;

    public SolicitanteResponse(Long idSolicitante, String nombreCompleto, String numeroDocumento,
            LocalDateTime fechaRegistro) {
        this.idSolicitante = idSolicitante;
        this.nombreCompleto = nombreCompleto;
        this.numeroDocumento = numeroDocumento;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getIdSolicitante() {
        return idSolicitante;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
