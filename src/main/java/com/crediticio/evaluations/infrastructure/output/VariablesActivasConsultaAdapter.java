package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.domain.NombreVariableEvaluada;
import com.crediticio.evaluations.ports.output.ConsultarVariablesActivasEvaluacionPort;
import com.crediticio.evaluations.ports.output.VariableActivaEvaluada;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Único componente de {@code evaluations} que conoce el modelo de dominio de
 * {@code riskvariables}. Traduce esa información hacia el contrato propio de
 * {@code evaluations} ({@link ConsultarVariablesActivasEvaluacionPort}), reutilizando el
 * puerto de persistencia ya existente en {@code riskvariables} sin duplicarlo.
 */
@Component
public class VariablesActivasConsultaAdapter implements ConsultarVariablesActivasEvaluacionPort {

    private final VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    public VariablesActivasConsultaAdapter(VariableRiesgoRepositoryPort variableRiesgoRepositoryPort) {
        this.variableRiesgoRepositoryPort = variableRiesgoRepositoryPort;
    }

    @Override
    public List<VariableActivaEvaluada> listarActivas() {
        return variableRiesgoRepositoryPort.listarActivas().stream()
                .map(this::aProyeccion)
                .toList();
    }

    private VariableActivaEvaluada aProyeccion(VariableRiesgo variableRiesgo) {
        NombreVariableEvaluada variable = NombreVariableEvaluada.valueOf(variableRiesgo.getVariable().name());
        return new VariableActivaEvaluada(variableRiesgo.getIdVariableRiesgo(), variable);
    }
}
