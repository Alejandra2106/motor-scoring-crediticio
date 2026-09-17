package com.crediticio.applicants.application;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteResponse;
import com.crediticio.applicants.domain.Solicitante;
import org.springframework.stereotype.Component;

@Component
public class SolicitanteMapper {

    public Solicitante aDominio(RegistrarSolicitanteRequest request) {
        return Solicitante.nuevo(
                request.getNombreCompleto(),
                request.getNumeroDocumento(),
                request.getIngresosMensuales(),
                request.getDeudasMensuales(),
                request.getNumeroMoras(),
                request.getHistorialCrediticio(),
                request.getAntiguedadLaboral());
    }

    public SolicitanteResponse aResponse(Solicitante solicitante) {
        return new SolicitanteResponse(
                solicitante.getIdSolicitante(),
                solicitante.getNombreCompleto(),
                solicitante.getNumeroDocumento(),
                solicitante.getFechaRegistro());
    }
}
