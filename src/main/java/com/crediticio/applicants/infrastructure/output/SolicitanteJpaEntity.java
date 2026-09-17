package com.crediticio.applicants.infrastructure.output;

import com.crediticio.applicants.domain.HistorialCrediticio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitante")
public class SolicitanteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitante")
    private Long idSolicitante;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "numero_documento", nullable = false, unique = true, length = 15)
    private String numeroDocumento;

    @Column(name = "ingresos_mensuales", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresosMensuales;

    @Column(name = "deudas_mensuales", nullable = false, precision = 15, scale = 2)
    private BigDecimal deudasMensuales;

    @Column(name = "numero_moras", nullable = false)
    private Integer numeroMoras;

    @Enumerated(EnumType.STRING)
    @Column(name = "historial_crediticio", nullable = false, length = 10)
    private HistorialCrediticio historialCrediticio;

    @Column(name = "antiguedad_laboral", nullable = false, precision = 5, scale = 2)
    private BigDecimal antiguedadLaboral;

    @Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    protected SolicitanteJpaEntity() {
        // requerido por JPA
    }

    public SolicitanteJpaEntity(String nombreCompleto, String numeroDocumento, BigDecimal ingresosMensuales,
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
