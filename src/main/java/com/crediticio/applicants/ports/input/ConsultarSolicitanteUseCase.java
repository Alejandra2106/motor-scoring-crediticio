package com.crediticio.applicants.ports.input;

import com.crediticio.applicants.application.dto.SolicitanteDetalleResponse;

public interface ConsultarSolicitanteUseCase {

    SolicitanteDetalleResponse consultarPorNumeroDocumento(String numeroDocumento);
}
