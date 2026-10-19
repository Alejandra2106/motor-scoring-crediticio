package com.crediticio.evaluations.application;

import com.crediticio.evaluations.domain.PoliticaReintentos;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import com.crediticio.evaluations.ports.output.ResultadoReclamo;
import com.crediticio.evaluations.ports.output.TrabajoRecomendacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Proceso de reenvío de la saga (HU14 CA14, CA16): reclama de uno en uno los trabajos pendientes cuya espera
 * venció o cuya concesión expiró (por ejemplo, tras una caída), y vuelve a intentar la generación. Se ejecuta
 * de forma periódica; el estado necesario para reanudar vive en la outbox, no en memoria.
 */
@Service
public class ReenvioRecomendacionesService {

    private static final Logger log = LoggerFactory.getLogger(ReenvioRecomendacionesService.class);

    private final TrabajoRecomendacionPort trabajoPort;
    private final ProcesoGeneracionRecomendaciones proceso;
    private final PoliticaReintentos politica;

    public ReenvioRecomendacionesService(TrabajoRecomendacionPort trabajoPort, ProcesoGeneracionRecomendaciones proceso,
            PoliticaReintentos politica) {
        this.trabajoPort = trabajoPort;
        this.proceso = proceso;
        this.politica = politica;
    }

    /** @return número de trabajos procesados (reclamados o agotados) en este ciclo */
    public int ejecutarCiclo(int maxTrabajos) {
        int procesados = 0;
        while (procesados < maxTrabajos) {
            ResultadoReclamo reclamo = trabajoPort.reclamarSiguiente(politica.getMaxIntentos(), UUID.randomUUID(),
                    politica.getConcesion());
            switch (reclamo) {
                case ResultadoReclamo.Ninguno ninguno -> {
                    return procesados;
                }
                case ResultadoReclamo.Agotado agotado -> log.error(
                        "Saga de recomendaciones: la evaluación {} agotó sus concesiones; requiere intervención",
                        agotado.idEvaluacion());
                case ResultadoReclamo.Reclamado reclamado -> proceso.intentar(reclamado.idEvaluacion(),
                        reclamado.token(), ModoIntento.REENVIO, reclamado.intentos());
            }
            procesados++;
        }
        return procesados;
    }
}
