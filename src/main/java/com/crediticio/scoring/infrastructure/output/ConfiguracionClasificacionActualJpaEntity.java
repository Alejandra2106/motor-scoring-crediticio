package com.crediticio.scoring.infrastructure.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_clasificacion_actual")
public class ConfiguracionClasificacionActualJpaEntity {

    @Id
    @Column(name = "id_configuracion")
    private Short idConfiguracion;

    @Column(name = "id_politica")
    private Long idPolitica;

    protected ConfiguracionClasificacionActualJpaEntity() {
        // requerido por JPA
    }

    public Short getIdConfiguracion() {
        return idConfiguracion;
    }

    public Long getIdPolitica() {
        return idPolitica;
    }

    public void setIdPolitica(Long idPolitica) {
        this.idPolitica = idPolitica;
    }
}
