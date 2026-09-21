package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.CrearReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.domain.ReglaScoringVariableInactivaException;
import com.crediticio.scoring.domain.ReglaScoringVariableNoEncontradaException;
import com.crediticio.scoring.ports.input.CrearReglaScoringUseCase;
import com.crediticio.scoring.ports.output.ConsultarVariableRiesgoPort;
import com.crediticio.scoring.ports.output.ReglaScoringRepositoryPort;
import com.crediticio.scoring.ports.output.VariableRiesgoConsultada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearReglaScoringService implements CrearReglaScoringUseCase {

    private final ConsultarVariableRiesgoPort consultarVariableRiesgoPort;
    private final ReglaScoringRepositoryPort reglaScoringRepositoryPort;
    private final ReglaScoringMapper reglaScoringMapper;

    public CrearReglaScoringService(ConsultarVariableRiesgoPort consultarVariableRiesgoPort,
            ReglaScoringRepositoryPort reglaScoringRepositoryPort, ReglaScoringMapper reglaScoringMapper) {
        this.consultarVariableRiesgoPort = consultarVariableRiesgoPort;
        this.reglaScoringRepositoryPort = reglaScoringRepositoryPort;
        this.reglaScoringMapper = reglaScoringMapper;
    }

    @Override
    @Transactional
    public ReglaScoringResponse crear(CrearReglaScoringRequest request) {
        VariableRiesgoConsultada variable = consultarVariableRiesgoPort.consultar(request.getIdRiesgo())
                .orElseThrow(ReglaScoringVariableNoEncontradaException::new);

        if (!variable.activa()) {
            throw new ReglaScoringVariableInactivaException();
        }

        ReglaScoring reglaScoring = ReglaScoring.nueva(
                request.getIdRiesgo(),
                request.getOperador(),
                request.getValorCondicion(),
                request.getPuntaje(),
                variable.tipo(),
                variable.permiteDecimales());

        if (reglaScoringRepositoryPort.existeCombinacion(
                reglaScoring.getIdRiesgo(), reglaScoring.getOperador(), reglaScoring.getValorCondicion())) {
            throw new ReglaScoringDuplicadaException();
        }

        ReglaScoring reglaScoringGuardada = reglaScoringRepositoryPort.guardar(reglaScoring);
        return reglaScoringMapper.aResponse(reglaScoringGuardada);
    }
}
