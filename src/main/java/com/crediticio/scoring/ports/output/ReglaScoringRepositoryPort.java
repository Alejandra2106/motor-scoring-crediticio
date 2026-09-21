package com.crediticio.scoring.ports.output;

import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;

public interface ReglaScoringRepositoryPort {

    ReglaScoring guardar(ReglaScoring reglaScoring);

    boolean existeCombinacion(Long idRiesgo, OperadorScoring operador, String valorCondicion);
}
