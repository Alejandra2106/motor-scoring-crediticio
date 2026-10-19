package com.crediticio.scoring.infrastructure.output;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ConfiguracionClasificacionActualJpaRepository
        extends JpaRepository<ConfiguracionClasificacionActualJpaEntity, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConfiguracionClasificacionActualJpaEntity> findByIdConfiguracion(Short idConfiguracion);
}
