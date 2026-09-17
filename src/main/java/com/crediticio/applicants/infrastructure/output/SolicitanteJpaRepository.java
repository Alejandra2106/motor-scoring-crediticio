package com.crediticio.applicants.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitanteJpaRepository extends JpaRepository<SolicitanteJpaEntity, Long> {

    boolean existsByNumeroDocumento(String numeroDocumento);
}
