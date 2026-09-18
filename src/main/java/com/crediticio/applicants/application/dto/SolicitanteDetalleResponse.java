package com.crediticio.applicants.application.dto;

import com.crediticio.applicants.domain.HistorialCrediticio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SolicitanteDetalleResponse {

    private final Long idSolicitante;
    private final String nombreCompleto;
    private final String numeroDocumento;
    private final BigDecimal ingresosMensuales;
    private final BigDecimal deudasMensuales;
    private final Integer numeroMoras;
    private final HistorialCrediticio historialCrediticio;
    private final BigDecimal antiguedadLaboral;
    private final LocalDateTime fechaRegistro;

    public SolicitanteDetalleResponse(Long idSolicitante, String nombreCompleto, String numeroDocumento,
            BigDecimal ingresosMensuales, BigDecimal deudasMensuales, Integer numeroMoras,
            HistorialCrediticio historialCrediticio, BigDecimal antiguedadLaboral, LocalDateTime fechaRegistro) {
        this.idSolicitante = idSolicitante;
        this.nombreCompleto = nombreCompleto;
        this.numeroDocumento = numeroDocumento;
        this.ingresosMensuales = ingresosMensuales;
        this.deudasMensuales = deudasMensuales;
        this.numeroMoras = numeroMoras;
        this.historialCrediticio = historialCrediticio;
        this.antiguedadLaboral = antiguedadLaboral;
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

    public BigDecimal getIngresosMensuales() {
        return ingresosMensuales;
    }

    public BigDecimal getDeudasMensuales() {
        return deudasMensuales;
    }

    public Integer getNumeroMoras() {
        return numeroMoras;
    }

    public HistorialCrediticio getHistorialCrediticio() {
        return historialCrediticio;
    }

    public BigDecimal getAntiguedadLaboral() {
        return antiguedadLaboral;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
