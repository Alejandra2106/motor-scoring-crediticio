package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.domain.ReglaScoring;
import org.springframework.stereotype.Component;

@Component
public class ReglaScoringMapper {

    public ReglaScoringResponse aResponse(ReglaScoring reglaScoring) {
        return new ReglaScoringResponse(
                reglaScoring.getIdRegla(),
                reglaScoring.getIdRiesgo(),
                reglaScoring.getOperador(),
                reglaScoring.getValorCondicion(),
                reglaScoring.getPuntaje());
    }
}
