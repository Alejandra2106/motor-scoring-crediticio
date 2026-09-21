package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.applicants.domain.Solicitante;
import com.crediticio.applicants.ports.output.SolicitanteRepositoryPort;
import com.crediticio.evaluations.domain.SolicitanteParaEvaluacion;
import com.crediticio.evaluations.ports.output.ConsultarSolicitanteEvaluacionPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Único componente de {@code evaluations} que conoce el modelo de dominio de {@code applicants}.
 * Traduce esa información hacia el contrato propio de {@code evaluations}
 * ({@link ConsultarSolicitanteEvaluacionPort}), reutilizando el puerto de persistencia ya
 * existente en {@code applicants} sin duplicarlo (mismo patrón que
 * {@code scoring.infrastructure.output.VariableRiesgoConsultaAdapter} en HU05).
 */
@Component
public class SolicitanteConsultaAdapter implements ConsultarSolicitanteEvaluacionPort {

    private final SolicitanteRepositoryPort solicitanteRepositoryPort;

    public SolicitanteConsultaAdapter(SolicitanteRepositoryPort solicitanteRepositoryPort) {
        this.solicitanteRepositoryPort = solicitanteRepositoryPort;
    }

    @Override
    public Optional<SolicitanteParaEvaluacion> buscarPorId(Long idSolicitante) {
        return solicitanteRepositoryPort.buscarPorId(idSolicitante).map(this::aProyeccion);
    }

    @Override
    public Optional<SolicitanteParaEvaluacion> buscarPorNumeroDocumento(String numeroDocumento) {
        return solicitanteRepositoryPort.buscarPorNumeroDocumento(numeroDocumento).map(this::aProyeccion);
    }

    private SolicitanteParaEvaluacion aProyeccion(Solicitante solicitante) {
        return new SolicitanteParaEvaluacion(
                solicitante.getIdSolicitante(),
                solicitante.getIngresosMensuales(),
                solicitante.getDeudasMensuales(),
                solicitante.getNumeroMoras(),
                solicitante.getHistorialCrediticio().name(),
                solicitante.getAntiguedadLaboral());
    }
}
