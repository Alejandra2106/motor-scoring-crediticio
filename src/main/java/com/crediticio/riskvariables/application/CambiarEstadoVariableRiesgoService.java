package com.crediticio.riskvariables.application;

import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoResponse;
import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgoNoEncontradaException;
import com.crediticio.riskvariables.ports.input.CambiarEstadoVariableRiesgoUseCase;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CambiarEstadoVariableRiesgoService implements CambiarEstadoVariableRiesgoUseCase {

    private static final Logger log = LoggerFactory.getLogger(CambiarEstadoVariableRiesgoService.class);

    private final VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    public CambiarEstadoVariableRiesgoService(VariableRiesgoRepositoryPort variableRiesgoRepositoryPort) {
        this.variableRiesgoRepositoryPort = variableRiesgoRepositoryPort;
    }

    @Override
    @Transactional
    public CambiarEstadoVariableRiesgoResponse cambiarEstado(Long idRiesgo, CambiarEstadoVariableRiesgoRequest request) {
        VariableRiesgo variableRiesgo = variableRiesgoRepositoryPort.buscarPorId(idRiesgo)
                .orElseThrow(VariableRiesgoNoEncontradaException::new);

        EstadoVariableRiesgo estadoAnterior = variableRiesgo.getEstado();
        VariableRiesgo variableRiesgoActualizada = variableRiesgo.cambiarEstado(request.getEstado());
        variableRiesgoRepositoryPort.guardar(variableRiesgoActualizada);

        log.info("Cambio de estado de variable de riesgo idRiesgo={} estadoAnterior={} estadoNuevo={} traceId={}",
                idRiesgo, estadoAnterior, variableRiesgoActualizada.getEstado(), MDC.get("traceId"));

        return new CambiarEstadoVariableRiesgoResponse(idRiesgo, estadoAnterior, variableRiesgoActualizada.getEstado());
    }
}
