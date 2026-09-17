package com.crediticio.applicants.application;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteResponse;
import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.ports.input.RegistrarSolicitanteUseCase;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarSolicitanteService implements RegistrarSolicitanteUseCase {

    private final SolicitanteRepositoryPort solicitanteRepositoryPort;
    private final SolicitanteMapper solicitanteMapper;

    public RegistrarSolicitanteService(SolicitanteRepositoryPort solicitanteRepositoryPort,
            SolicitanteMapper solicitanteMapper) {
        this.solicitanteRepositoryPort = solicitanteRepositoryPort;
        this.solicitanteMapper = solicitanteMapper;
    }

    @Override
    @Transactional
    public SolicitanteResponse registrar(RegistrarSolicitanteRequest request) {
        if (solicitanteRepositoryPort.existePorNumeroDocumento(request.getNumeroDocumento())) {
            throw new DocumentoDuplicadoException();
        }
        Solicitante solicitante = solicitanteMapper.aDominio(request);
        Solicitante solicitanteGuardado = solicitanteRepositoryPort.guardar(solicitante);
        return solicitanteMapper.aResponse(solicitanteGuardado);
    }
}
