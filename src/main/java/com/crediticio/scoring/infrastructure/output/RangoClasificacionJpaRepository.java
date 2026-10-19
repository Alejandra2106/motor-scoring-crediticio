package com.crediticio.scoring.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RangoClasificacionJpaRepository extends JpaRepository<RangoClasificacionJpaEntity, Long> {

    List<RangoClasificacionJpaEntity> findByIdPoliticaOrderByLimiteInferior(Long idPolitica);
}
