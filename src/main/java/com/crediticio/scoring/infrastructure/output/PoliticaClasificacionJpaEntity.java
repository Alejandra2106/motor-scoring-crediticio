package com.crediticio.scoring.infrastructure.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Todas las columnas son {@code updatable = false} y la entidad no tiene setters: una política
 * publicada nunca genera UPDATE desde Hibernate. publicado_por es nulo en las versiones publicadas
 * antes de que se registrara el autor (CA14).
 */
@Entity
@Table(name = "politica_clasificacion")
public class PoliticaClasificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_politica")
    private Long idPolitica;

    @Column(name = "version", nullable = false, updatable = false)
    private Integer numeroVersion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "publicado_por", updatable = false)
    private UUID publicadoPor;

    protected PoliticaClasificacionJpaEntity() {
        // requerido por JPA
    }

    public PoliticaClasificacionJpaEntity(Integer numeroVersion, LocalDateTime fechaCreacion,
            LocalDateTime fechaPublicacion, UUID publicadoPor) {
        this.numeroVersion = numeroVersion;
        this.fechaCreacion = fechaCreacion;
        this.fechaPublicacion = fechaPublicacion;
        this.publicadoPor = publicadoPor;
    }

    public Long getIdPolitica() {
        return idPolitica;
    }

    public Integer getNumeroVersion() {
        return numeroVersion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public UUID getPublicadoPor() {
        return publicadoPor;
    }
}
