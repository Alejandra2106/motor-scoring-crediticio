package com.crediticio.evaluations.infrastructure.output;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EvaluacionJpaRepository extends JpaRepository<EvaluacionJpaEntity, Long> {

    /**
     * Versión de la política referenciada por una evaluación (HU12 CA09). Las políticas publicadas
     * son inmutables (HU11 CA11), así que la versión leída es la que se usó al clasificar.
     */
    @Query(value = "SELECT version FROM politica_clasificacion WHERE id_politica = :idPolitica", nativeQuery = true)
    Optional<Integer> buscarVersionPolitica(@Param("idPolitica") Long idPolitica);
}
