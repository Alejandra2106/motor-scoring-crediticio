package com.crediticio.applicants.ports.input;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteResponse;

public interface RegistrarSolicitanteUseCase {

    SolicitanteResponse registrar(RegistrarSolicitanteRequest request);
}
