package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.domain.NombreVariableEvaluada;
import com.crediticio.evaluations.domain.SolicitanteEvaluacionNoEncontradoException;
import com.crediticio.evaluations.domain.SolicitanteParaEvaluacion;
import com.crediticio.evaluations.domain.ValorVariableResolver;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.output.ConsultarSolicitanteEvaluacionPort;
import com.crediticio.evaluations.ports.output.ConsultarVariablesActivasEvaluacionPort;
import com.crediticio.evaluations.ports.output.EvaluacionRepositoryPort;
import com.crediticio.evaluations.ports.output.ReglasScoringConsultaPort;
import com.crediticio.evaluations.ports.output.ResultadoReglaEvaluada;
import com.crediticio.evaluations.ports.output.VariableActivaEvaluada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalcularScoreService implements CalcularScoreUseCase {

    private final ConsultarSolicitanteEvaluacionPort consultarSolicitantePort;
    private final ConsultarVariablesActivasEvaluacionPort consultarVariablesActivasPort;
    private final ReglasScoringConsultaPort reglasScoringConsultaPort;
    private final EvaluacionRepositoryPort evaluacionRepositoryPort;
    private final EvaluacionMapper evaluacionMapper;

    public CalcularScoreService(ConsultarSolicitanteEvaluacionPort consultarSolicitantePort,
            ConsultarVariablesActivasEvaluacionPort consultarVariablesActivasPort,
            ReglasScoringConsultaPort reglasScoringConsultaPort, EvaluacionRepositoryPort evaluacionRepositoryPort,
            EvaluacionMapper evaluacionMapper) {
        this.consultarSolicitantePort = consultarSolicitantePort;
        this.consultarVariablesActivasPort = consultarVariablesActivasPort;
        this.reglasScoringConsultaPort = reglasScoringConsultaPort;
        this.evaluacionRepositoryPort = evaluacionRepositoryPort;
        this.evaluacionMapper = evaluacionMapper;
    }

    @Override
    @Transactional
    public EvaluacionResponse calcular(CalcularScoreRequest request) {
        SolicitanteParaEvaluacion solicitante = identificarSolicitante(request);
        List<DetalleEvaluacion> detalles = evaluarReglasVigentes(solicitante);
        Evaluacion evaluacion = Evaluacion.nueva(solicitante.idSolicitante(), detalles);
        return evaluacionMapper.aResponse(evaluacionRepositoryPort.guardar(evaluacion));
    }

    private SolicitanteParaEvaluacion identificarSolicitante(CalcularScoreRequest request) {
        if (request.getIdSolicitante() != null) {
            return consultarSolicitantePort.buscarPorId(request.getIdSolicitante())
                    .orElseThrow(SolicitanteEvaluacionNoEncontradoException::new);
        }
        return consultarSolicitantePort.buscarPorNumeroDocumento(request.getNumeroDocumento())
                .orElseThrow(SolicitanteEvaluacionNoEncontradoException::new);
    }

    private List<DetalleEvaluacion> evaluarReglasVigentes(SolicitanteParaEvaluacion solicitante) {
        List<VariableActivaEvaluada> variablesActivas = consultarVariablesActivasPort.listarActivas();
        if (variablesActivas.isEmpty()) {
            return List.of();
        }

        Map<Long, NombreVariableEvaluada> variablePorRiesgo = new HashMap<>();
        for (VariableActivaEvaluada variableActiva : variablesActivas) {
            variablePorRiesgo.put(variableActiva.idRiesgo(), variableActiva.variable());
        }

        Set<Long> idsConReglas = reglasScoringConsultaPort.idsConReglasVigentes(variablePorRiesgo.keySet());
        if (idsConReglas.isEmpty()) {
            return List.of();
        }

        Map<Long, String> valorRealPorRiesgo = new HashMap<>();
        for (Long idRiesgo : idsConReglas) {
            NombreVariableEvaluada variable = variablePorRiesgo.get(idRiesgo);
            valorRealPorRiesgo.put(idRiesgo, ValorVariableResolver.resolver(variable, solicitante));
        }

        List<ResultadoReglaEvaluada> resultados = reglasScoringConsultaPort.evaluar(valorRealPorRiesgo);
        return resultados.stream()
                .map(resultado -> DetalleEvaluacion.nuevo(
                        resultado.idRegla(),
                        resultado.operadorAplicado(),
                        resultado.valorCondicionAplicado(),
                        resultado.puntajeReglaAplicado(),
                        resultado.condicionCumplida(),
                        resultado.puntajeObtenido()))
                .toList();
    }
}
