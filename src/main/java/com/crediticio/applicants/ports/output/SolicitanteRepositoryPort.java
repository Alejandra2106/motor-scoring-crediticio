package com.crediticio.applicants.ports.output;

import com.crediticio.applicants.domain.Solicitante;

public interface SolicitanteRepositoryPort {

    Solicitante guardar(Solicitante solicitante);

    boolean existePorNumeroDocumento(String numeroDocumento);
}
