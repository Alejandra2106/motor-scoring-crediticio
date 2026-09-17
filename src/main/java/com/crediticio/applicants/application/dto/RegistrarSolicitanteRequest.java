package com.crediticio.applicants.application.dto;

import com.crediticio.applicants.domain.HistorialCrediticio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RegistrarSolicitanteRequest {

    @NotBlank(message = "nombreCompleto es obligatorio")
    @Size(min = 3, max = 100, message = "nombreCompleto debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "numeroDocumento es obligatorio")
    @Pattern(regexp = "\\d{6,15}", message = "numeroDocumento debe contener entre 6 y 15 dígitos")
    private String numeroDocumento;

    @NotNull(message = "ingresosMensuales es obligatorio")
    @DecimalMin(value = "0", message = "ingresosMensuales debe ser mayor o igual a 0")
    private BigDecimal ingresosMensuales;

    @NotNull(message = "deudasMensuales es obligatorio")
    @DecimalMin(value = "0", message = "deudasMensuales debe ser mayor o igual a 0")
    private BigDecimal deudasMensuales;

    @NotNull(message = "numeroMoras es obligatorio")
    @Min(value = 0, message = "numeroMoras debe ser mayor o igual a 0")
    private Integer numeroMoras;

    @NotNull(message = "historialCrediticio es obligatorio")
    private HistorialCrediticio historialCrediticio;

    @NotNull(message = "antiguedadLaboral es obligatorio")
    @DecimalMin(value = "0", message = "antiguedadLaboral debe ser mayor o igual a 0")
    private BigDecimal antiguedadLaboral;

    public RegistrarSolicitanteRequest() {
    }

    public RegistrarSolicitanteRequest(String nombreCompleto, String numeroDocumento, BigDecimal ingresosMensuales,
            BigDecimal deudasMensuales, Integer numeroMoras, HistorialCrediticio historialCrediticio,
            BigDecimal antiguedadLaboral) {
        this.nombreCompleto = nombreCompleto;
        this.numeroDocumento = numeroDocumento;
        this.ingresosMensuales = ingresosMensuales;
        this.deudasMensuales = deudasMensuales;
        this.numeroMoras = numeroMoras;
        this.historialCrediticio = historialCrediticio;
        this.antiguedadLaboral = antiguedadLaboral;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public BigDecimal getIngresosMensuales() {
        return ingresosMensuales;
    }

    public void setIngresosMensuales(BigDecimal ingresosMensuales) {
        this.ingresosMensuales = ingresosMensuales;
    }

    public BigDecimal getDeudasMensuales() {
        return deudasMensuales;
    }

    public void setDeudasMensuales(BigDecimal deudasMensuales) {
        this.deudasMensuales = deudasMensuales;
    }

    public Integer getNumeroMoras() {
        return numeroMoras;
    }

    public void setNumeroMoras(Integer numeroMoras) {
        this.numeroMoras = numeroMoras;
    }

    public HistorialCrediticio getHistorialCrediticio() {
        return historialCrediticio;
    }

    public void setHistorialCrediticio(HistorialCrediticio historialCrediticio) {
        this.historialCrediticio = historialCrediticio;
    }

    public BigDecimal getAntiguedadLaboral() {
        return antiguedadLaboral;
    }

    public void setAntiguedadLaboral(BigDecimal antiguedadLaboral) {
        this.antiguedadLaboral = antiguedadLaboral;
    }
}
