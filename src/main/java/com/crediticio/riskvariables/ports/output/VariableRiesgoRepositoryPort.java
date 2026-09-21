package com.crediticio.riskvariables.ports.output;

import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;

import java.util.List;
import java.util.Optional;

public interface VariableRiesgoRepositoryPort {

    VariableRiesgo guardar(VariableRiesgo variableRiesgo);

    boolean existePorVariable(NombreVariableRiesgo variable);

    Optional<VariableRiesgo> buscarPorId(Long idVariableRiesgo);

    List<VariableRiesgo> listarActivas();
}
