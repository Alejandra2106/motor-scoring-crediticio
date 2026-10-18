package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.ClasificacionRiesgoPort;
import com.crediticio.evaluations.ports.output.ResultadoClasificacion;
import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.scoring.ports.output.PoliticaClasificacionConsultaPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Traduce la política de clasificación de {@code scoring} al contrato de {@code evaluations}
 * ({@link ClasificacionRiesgoPort}), reutilizando el puerto de consulta de HU11 y la regla de
 * clasificación del dominio de {@code scoring} sin duplicarla (HU12 CA01). La política se lee una
 * única vez por evaluación, de modo que la categoría y la versión referenciada provienen siempre de
 * la misma versión (CA02).
 */
@Component
public class ClasificacionRiesgoAdapter implements ClasificacionRiesgoPort {

    private final PoliticaClasificacionConsultaPort politicaClasificacionConsultaPort;

    public ClasificacionRiesgoAdapter(PoliticaClasificacionConsultaPort politicaClasificacionConsultaPort) {
        this.politicaClasificacionConsultaPort = politicaClasificacionConsultaPort;
    }

    @Override
    public Optional<ResultadoClasificacion> clasificarConPoliticaVigente(int puntajeTotal) {
        return politicaClasificacionConsultaPort.buscarVigente()
                .map(politica -> new ResultadoClasificacion(
                        politica.getIdPolitica(),
                        politica.getVersion(),
                        politica.clasificar(puntajeTotal).map(CategoriaRiesgo::name).orElse(null)));
    }
}
