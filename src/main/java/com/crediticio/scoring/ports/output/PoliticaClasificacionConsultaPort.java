package com.crediticio.scoring.ports.output;

import com.crediticio.scoring.domain.PoliticaClasificacion;

import java.util.Optional;

/**
 * Consulta de solo lectura, separada del puerto de publicación. Devuelve vacío cuando la
 * configuración todavía no referencia ninguna política vigente.
 */
public interface PoliticaClasificacionConsultaPort {

    Optional<PoliticaClasificacion> buscarVigente();
}
