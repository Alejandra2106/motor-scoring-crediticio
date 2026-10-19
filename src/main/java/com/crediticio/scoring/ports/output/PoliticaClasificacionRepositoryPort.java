package com.crediticio.scoring.ports.output;

import com.crediticio.scoring.domain.PoliticaClasificacion;

/**
 * Las políticas publicadas son inmutables: este puerto solo permite publicar una nueva versión
 * (política, rangos y política vigente en una misma operación); no ofrece actualizaciones ni
 * eliminaciones. Esto no impide modificaciones mediante SQL directo sobre la base de datos.
 */
public interface PoliticaClasificacionRepositoryPort {

    PoliticaClasificacion publicar(PoliticaClasificacion politica);
}
