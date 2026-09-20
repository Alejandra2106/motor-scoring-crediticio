package com.crediticio.scoring.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReglaScoringJpaRepository extends JpaRepository<ReglaScoringJpaEntity, Long> {

    boolean existsByIdRiesgoAndOperadorAndValorCondicion(Long idRiesgo, String operador, String valorCondicion);
}
