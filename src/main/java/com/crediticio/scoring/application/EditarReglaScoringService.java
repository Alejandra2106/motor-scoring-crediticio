package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.EditarReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.domain.ReglaScoringNoEncontradaException;
import com.crediticio.scoring.domain.ReglaScoringVariableInactivaException;
import com.crediticio.scoring.domain.ReglaScoringVariableNoEncontradaException;
import com.crediticio.scoring.ports.input.EditarReglaScoringUseCase;
import com.crediticio.scoring.ports.output.ConsultarVariableRiesgoPort;
import com.crediticio.scoring.ports.output.ReglaScoringRepositoryPort;
import com.crediticio.scoring.ports.output.VariableRiesgoConsultada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarReglaScoringService implements EditarReglaScoringUseCase {

    private final ConsultarVariableRiesgoPort consultarVariableRiesgoPort;
    private final ReglaScoringRepositoryPort reglaScoringRepositoryPort;
    private final ReglaScoringMapper reglaScoringMapper;

    public EditarReglaScoringService(ConsultarVariableRiesgoPort consultarVariableRiesgoPort,
            ReglaScoringRepositoryPort reglaScoringRepositoryPort, ReglaScoringMapper reglaScoringMapper) {
        this.consultarVariableRiesgoPort = consultarVariableRiesgoPort;
        this.reglaScoringRepositoryPort = reglaScoringRepositoryPort;
        this.reglaScoringMapper = reglaScoringMapper;
    }

    @Override
    @Transactional
    public ReglaScoringResponse editar(Long idRegla, EditarReglaScoringRequest request) {
        ReglaScoring reglaExistente = reglaScoringRepositoryPort.buscarPorId(idRegla)
                .orElseThrow(ReglaScoringNoEncontradaException::new);

        VariableRiesgoConsultada variable = consultarVariableRiesgoPort.consultar(reglaExistente.getIdRiesgo())
                .orElseThrow(ReglaScoringVariableNoEncontradaException::new);

        if (!variable.activa()) {
            throw new ReglaScoringVariableInactivaException();
        }

        ReglaScoring reglaEditada = reglaExistente.editar(
                request.getOperador(),
                request.getValorCondicion(),
                request.getPuntaje(),
                variable.tipo(),
                variable.permiteDecimales());

        if (reglaScoringRepositoryPort.existeCombinacion(
                reglaEditada.getIdRiesgo(), reglaEditada.getOperador(), reglaEditada.getValorCondicion(), idRegla)) {
            throw new ReglaScoringDuplicadaException();
        }

        ReglaScoring reglaGuardada = reglaScoringRepositoryPort.guardar(reglaEditada);
        return reglaScoringMapper.aResponse(reglaGuardada);
    }
}
