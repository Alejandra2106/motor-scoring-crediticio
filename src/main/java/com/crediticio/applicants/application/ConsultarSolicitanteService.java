package com.crediticio.applicants.application;

import com.crediticio.applicants.application.dto.SolicitanteDetalleResponse;
import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.domain.SolicitanteNoEncontradoException;
import com.crediticio.applicants.ports.input.ConsultarSolicitanteUseCase;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarSolicitanteService implements ConsultarSolicitanteUseCase {

    private final SolicitanteRepositoryPort solicitanteRepositoryPort;
    private final SolicitanteMapper solicitanteMapper;

    public ConsultarSolicitanteService(SolicitanteRepositoryPort solicitanteRepositoryPort,
            SolicitanteMapper solicitanteMapper) {
        this.solicitanteRepositoryPort = solicitanteRepositoryPort;
        this.solicitanteMapper = solicitanteMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitanteDetalleResponse consultarPorNumeroDocumento(String numeroDocumento) {
        Solicitante solicitante = solicitanteRepositoryPort.buscarPorNumeroDocumento(numeroDocumento)
                .orElseThrow(SolicitanteNoEncontradoException::new);
        return solicitanteMapper.aDetalleResponse(solicitante);
    }
}
