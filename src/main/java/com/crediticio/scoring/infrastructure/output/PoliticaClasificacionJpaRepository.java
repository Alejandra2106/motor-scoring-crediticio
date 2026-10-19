package com.crediticio.scoring.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PoliticaClasificacionJpaRepository extends JpaRepository<PoliticaClasificacionJpaEntity, Long> {

    @Query("select max(p.numeroVersion) from PoliticaClasificacionJpaEntity p")
    Optional<Integer> buscarVersionMaxima();
}
