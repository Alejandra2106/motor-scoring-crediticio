package com.crediticio.evaluations.ports.output;

import com.crediticio.evaluations.domain.SolicitanteParaEvaluacion;

import java.util.Optional;

public interface ConsultarSolicitanteEvaluacionPort {

    Optional<SolicitanteParaEvaluacion> buscarPorId(Long idSolicitante);

    Optional<SolicitanteParaEvaluacion> buscarPorNumeroDocumento(String numeroDocumento);
}
