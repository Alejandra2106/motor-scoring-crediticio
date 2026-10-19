package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;
import com.crediticio.scoring.application.dto.RangoClasificacionRequest;
import com.crediticio.scoring.application.dto.RangoClasificacionResponse;
import com.crediticio.scoring.domain.DefinicionRango;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PoliticaClasificacionMapper {

    public List<DefinicionRango> aDefiniciones(PublicarPoliticaClasificacionRequest request) {
        if (request.getRangos() == null) {
            return null;
        }
        return request.getRangos().stream()
                .map(this::aDefinicion)
                .toList();
    }

    public PoliticaClasificacionResponse aResponse(PoliticaClasificacion politica) {
        List<RangoClasificacionResponse> rangos = politica.getRangos().stream()
                .map(rango -> new RangoClasificacionResponse(
                        rango.getIdRango(),
                        rango.getCategoria(),
                        rango.getLimiteInferior(),
                        rango.getLimiteSuperior()))
                .toList();
        return new PoliticaClasificacionResponse(
                politica.getIdPolitica(),
                politica.getVersion(),
                politica.getFechaCreacion(),
                politica.getFechaPublicacion(),
                rangos);
    }

    private DefinicionRango aDefinicion(RangoClasificacionRequest rango) {
        if (rango == null) {
            return null;
        }
        return new DefinicionRango(rango.getCategoria(), rango.getLimiteInferior(), rango.getLimiteSuperior());
    }
}
