package com.crediticio.evaluations.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleEvaluacionJpaRepository extends JpaRepository<DetalleEvaluacionJpaEntity, Long> {

    List<DetalleEvaluacionJpaEntity> findByIdEvaluacion(Long idEvaluacion);
}
