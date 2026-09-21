package com.crediticio.scoring.ports.output;

import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;

import java.util.Optional;

public interface ReglaScoringRepositoryPort {

    ReglaScoring guardar(ReglaScoring reglaScoring);

    boolean existeCombinacion(Long idRiesgo, OperadorScoring operador, String valorCondicion);

    boolean existeCombinacion(Long idRiesgo, OperadorScoring operador, String valorCondicion, Long idReglaExcluida);

    Optional<ReglaScoring> buscarPorId(Long idRegla);
}
