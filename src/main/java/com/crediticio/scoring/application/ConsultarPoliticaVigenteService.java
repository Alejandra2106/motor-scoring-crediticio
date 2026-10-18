package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.domain.PoliticaVigenteNoDisponibleException;
import com.crediticio.scoring.ports.input.ConsultarPoliticaVigenteUseCase;
import com.crediticio.scoring.ports.output.PoliticaClasificacionConsultaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPoliticaVigenteService implements ConsultarPoliticaVigenteUseCase {

    private final PoliticaClasificacionConsultaPort politicaClasificacionConsultaPort;
    private final PoliticaClasificacionMapper politicaClasificacionMapper;

    public ConsultarPoliticaVigenteService(PoliticaClasificacionConsultaPort politicaClasificacionConsultaPort,
            PoliticaClasificacionMapper politicaClasificacionMapper) {
        this.politicaClasificacionConsultaPort = politicaClasificacionConsultaPort;
        this.politicaClasificacionMapper = politicaClasificacionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticaClasificacionResponse consultarVigente() {
        return politicaClasificacionConsultaPort.buscarVigente()
                .map(politicaClasificacionMapper::aResponse)
                .orElseThrow(PoliticaVigenteNoDisponibleException::new);
    }
}
