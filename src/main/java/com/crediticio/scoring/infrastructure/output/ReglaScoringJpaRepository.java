package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.domain.EstadoReglaScoring;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReglaScoringJpaRepository extends JpaRepository<ReglaScoringJpaEntity, Long> {

    boolean existsByIdRiesgoAndOperadorAndValorCondicion(Long idRiesgo, String operador, String valorCondicion);

    boolean existsByIdRiesgoAndOperadorAndValorCondicionAndIdReglaNot(Long idRiesgo, String operador,
            String valorCondicion, Long idRegla);

    List<ReglaScoringJpaEntity> findByIdRiesgoInAndEstado(Collection<Long> idsRiesgo, EstadoReglaScoring estado);
}
