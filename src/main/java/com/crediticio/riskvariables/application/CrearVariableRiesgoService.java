package com.crediticio.riskvariables.application;

import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import com.crediticio.riskvariables.ports.input.CrearVariableRiesgoUseCase;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearVariableRiesgoService implements CrearVariableRiesgoUseCase {

    private final VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;
    private final VariableRiesgoMapper variableRiesgoMapper;

    public CrearVariableRiesgoService(VariableRiesgoRepositoryPort variableRiesgoRepositoryPort,
            VariableRiesgoMapper variableRiesgoMapper) {
        this.variableRiesgoRepositoryPort = variableRiesgoRepositoryPort;
        this.variableRiesgoMapper = variableRiesgoMapper;
    }

    @Override
    @Transactional
    public VariableRiesgoResponse crear(CrearVariableRiesgoRequest request) {
        if (variableRiesgoRepositoryPort.existePorVariable(request.getVariable())) {
            throw new VariableRiesgoDuplicadaException();
        }
        VariableRiesgo variableRiesgo = variableRiesgoMapper.aDominio(request);
        VariableRiesgo variableRiesgoGuardada = variableRiesgoRepositoryPort.guardar(variableRiesgo);
        return variableRiesgoMapper.aResponse(variableRiesgoGuardada);
    }
}
