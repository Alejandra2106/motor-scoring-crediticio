package com.crediticio.evaluations.infrastructure.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trabajo de la saga de recomendaciones (HU14, tabla trabajo_recomendacion de V11). La entidad solo se usa
 * para crearlo junto con la evaluación; los reclamos y los registros de resultado usan sentencias propias
 * (TrabajoRecomendacionAdapter), con la hora de la base de datos y la barrera por token de concesión.
 */
@Entity
@Table(name = "trabajo_recomendacion")
public class TrabajoRecomendacionJpaEntity {

    @Id
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @Column(name = "estado_trabajo", nullable = false, length = 20, updatable = false)
    private String estadoTrabajo;

    @Column(name = "intentos", nullable = false, updatable = false)
    private int intentos;

    @Column(name = "intentos_totales", nullable = false, updatable = false)
    private int intentosTotales;

    @Column(name = "proximo_intento", nullable = false, updatable = false)
    private Instant proximoIntento;

    @Column(name = "id_concesion", updatable = false)
    private UUID idConcesion;

    @Column(name = "concesion_hasta", updatable = false)
    private Instant concesionHasta;

    @Column(name = "codigo_ultimo_error", length = 50, updatable = false)
    private String codigoUltimoError;

    @Column(name = "ultimo_error", length = 500, updatable = false)
    private String ultimoError;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false, updatable = false)
    private Instant actualizadoEn;

    @Column(name = "procesado_en", updatable = false)
    private Instant procesadoEn;

    protected TrabajoRecomendacionJpaEntity() {
    }

    /** Trabajo PENDIENTE con un intento (el intento en línea) y su concesión, como exige V11. */
    static TrabajoRecomendacionJpaEntity conConcesionInicial(long idEvaluacion, UUID token, Instant ahora,
            Instant concesionHasta) {
        TrabajoRecomendacionJpaEntity trabajo = new TrabajoRecomendacionJpaEntity();
        trabajo.idEvaluacion = idEvaluacion;
        trabajo.estadoTrabajo = "PENDIENTE";
        trabajo.intentos = 1;
        trabajo.intentosTotales = 1;
        trabajo.proximoIntento = ahora;
        trabajo.idConcesion = token;
        trabajo.concesionHasta = concesionHasta;
        trabajo.creadoEn = ahora;
        trabajo.actualizadoEn = ahora;
        return trabajo;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }
}
