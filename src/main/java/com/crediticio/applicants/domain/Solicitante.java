package com.crediticio.applicants.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Solicitante {

    private final Long idSolicitante;
    private final String nombreCompleto;
    private final String numeroDocumento;
    private final BigDecimal ingresosMensuales;
    private final BigDecimal deudasMensuales;
    private final Integer numeroMoras;
    private final HistorialCrediticio historialCrediticio;
    private final BigDecimal antiguedadLaboral;
    private final LocalDateTime fechaRegistro;

    private Solicitante(Long idSolicitante, String nombreCompleto, String numeroDocumento,
            BigDecimal ingresosMensuales, BigDecimal deudasMensuales, Integer numeroMoras,
            HistorialCrediticio historialCrediticio, BigDecimal antiguedadLaboral, LocalDateTime fechaRegistro) {
        this.idSolicitante = idSolicitante;
        this.nombreCompleto = validarNombreCompleto(nombreCompleto);
        this.numeroDocumento = validarNumeroDocumento(numeroDocumento);
        this.ingresosMensuales = validarNoNegativo(ingresosMensuales, "ingresosMensuales");
        this.deudasMensuales = validarNoNegativo(deudasMensuales, "deudasMensuales");
        this.numeroMoras = validarNumeroMoras(numeroMoras);
        this.historialCrediticio = Objects.requireNonNull(historialCrediticio, "historialCrediticio es obligatorio");
        this.antiguedadLaboral = validarNoNegativo(antiguedadLaboral, "antiguedadLaboral");
        this.fechaRegistro = fechaRegistro;
    }

    public static Solicitante nuevo(String nombreCompleto, String numeroDocumento, BigDecimal ingresosMensuales,
            BigDecimal deudasMensuales, Integer numeroMoras, HistorialCrediticio historialCrediticio,
            BigDecimal antiguedadLaboral) {
        return new Solicitante(null, nombreCompleto, numeroDocumento, ingresosMensuales, deudasMensuales,
                numeroMoras, historialCrediticio, antiguedadLaboral, null);
    }

    public static Solicitante reconstruir(Long idSolicitante, String nombreCompleto, String numeroDocumento,
            BigDecimal ingresosMensuales, BigDecimal deudasMensuales, Integer numeroMoras,
            HistorialCrediticio historialCrediticio, BigDecimal antiguedadLaboral, LocalDateTime fechaRegistro) {
        return new Solicitante(idSolicitante, nombreCompleto, numeroDocumento, ingresosMensuales, deudasMensuales,
                numeroMoras, historialCrediticio, antiguedadLaboral, fechaRegistro);
    }

    private static String validarNombreCompleto(String nombreCompleto) {
        if (nombreCompleto == null) {
            throw new IllegalArgumentException("nombreCompleto es obligatorio");
        }
        String nombreNormalizado = nombreCompleto.trim();
        if (nombreNormalizado.length() < 3 || nombreNormalizado.length() > 100) {
            throw new IllegalArgumentException("nombreCompleto debe tener entre 3 y 100 caracteres");
        }
        return nombreNormalizado;
    }

    private static String validarNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || !numeroDocumento.matches("\\d{6,15}")) {
            throw new IllegalArgumentException("numeroDocumento debe contener entre 6 y 15 dígitos");
        }
        return numeroDocumento;
    }

    private static Integer validarNumeroMoras(Integer numeroMoras) {
        if (numeroMoras == null || numeroMoras < 0) {
            throw new IllegalArgumentException("numeroMoras debe ser mayor o igual a 0");
        }
        return numeroMoras;
    }

    private static BigDecimal validarNoNegativo(BigDecimal valor, String nombreCampo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(nombreCampo + " debe ser mayor o igual a 0");
        }
        return valor;
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
