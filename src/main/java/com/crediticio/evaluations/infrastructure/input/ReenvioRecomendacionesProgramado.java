package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.config.SagaRecomendacionesProperties;
import com.crediticio.evaluations.application.ReenvioRecomendacionesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ejecuta periódicamente el proceso de reenvío de la saga (HU14), solo si está habilitada. */
@Component
@ConditionalOnProperty(prefix = "scoring.saga", name = "habilitada", havingValue = "true")
public class ReenvioRecomendacionesProgramado {

    private static final Logger log = LoggerFactory.getLogger(ReenvioRecomendacionesProgramado.class);

    private final ReenvioRecomendacionesService reenvio;
    private final int trabajosPorCiclo;

    public ReenvioRecomendacionesProgramado(ReenvioRecomendacionesService reenvio,
            SagaRecomendacionesProperties propiedades) {
        this.reenvio = reenvio;
        this.trabajosPorCiclo = propiedades.trabajosPorCiclo();
    }

    @Scheduled(fixedDelayString = "${scoring.saga.frecuencia-reenvio:PT5S}",
            initialDelayString = "${scoring.saga.frecuencia-reenvio:PT5S}")
    public void ejecutar() {
        try {
            reenvio.ejecutarCiclo(trabajosPorCiclo);
        } catch (RuntimeException ex) {
            // un fallo de un ciclo (base de datos no disponible, etc.) no detiene las ejecuciones siguientes
            log.error("Falló un ciclo del proceso de reenvío de la saga", ex);
        }
    }
}
