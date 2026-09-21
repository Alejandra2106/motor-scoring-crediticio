package com.crediticio.scoring.infrastructure.output;

import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.TipoVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.ports.output.ConsultarVariableRiesgoPort;
import com.crediticio.scoring.ports.output.VariableRiesgoConsultada;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Único componente de {@code scoring} que conoce el modelo de dominio de {@code riskvariables}.
 * Traduce esa información hacia el contrato propio de {@code scoring} ({@link ConsultarVariableRiesgoPort}),
 * reutilizando el puerto de persistencia ya existente en {@code riskvariables} sin duplicarlo.
 */
@Component
public class VariableRiesgoConsultaAdapter implements ConsultarVariableRiesgoPort {

    private final VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    public VariableRiesgoConsultaAdapter(VariableRiesgoRepositoryPort variableRiesgoRepositoryPort) {
        this.variableRiesgoRepositoryPort = variableRiesgoRepositoryPort;
    }

    @Override
    public Optional<VariableRiesgoConsultada> consultar(Long idRiesgo) {
        return variableRiesgoRepositoryPort.buscarPorId(idRiesgo).map(this::aConsulta);
    }

    private VariableRiesgoConsultada aConsulta(VariableRiesgo variableRiesgo) {
        boolean activa = variableRiesgo.getEstado() == EstadoVariableRiesgo.ACTIVA;
        TipoVariable tipo = variableRiesgo.getTipo() == TipoVariableRiesgo.NUMERICO
                ? TipoVariable.NUMERICO
                : TipoVariable.CATEGORICO;
        boolean permiteDecimales = variableRiesgo.getVariable() != NombreVariableRiesgo.NUMERO_MORAS;
        return new VariableRiesgoConsultada(activa, tipo, permiteDecimales);
    }
}
