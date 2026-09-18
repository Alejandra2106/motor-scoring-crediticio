package com.crediticio.applicants.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolicitanteJpaRepository extends JpaRepository<SolicitanteJpaEntity, Long> {

    boolean existsByNumeroDocumento(String numeroDocumento);

    Optional<SolicitanteJpaEntity> findByNumeroDocumento(String numeroDocumento);
}
