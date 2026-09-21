package com.crediticio.applicants.ports.output;

import com.crediticio.applicants.domain.Solicitante;

import java.util.Optional;

public interface SolicitanteRepositoryPort {

    Solicitante guardar(Solicitante solicitante);

    boolean existePorNumeroDocumento(String numeroDocumento);

    Optional<Solicitante> buscarPorNumeroDocumento(String numeroDocumento);

    Optional<Solicitante> buscarPorId(Long idSolicitante);
}
