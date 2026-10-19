package com.crediticio.config;

import com.crediticio.evaluations.domain.PoliticaReintentos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ThreadLocalRandom;

/** Saga de recomendaciones (HU14): política de reintentos y, si está habilitada, el proceso programado. */
@Configuration
@EnableConfigurationProperties(SagaRecomendacionesProperties.class)
public class SagaRecomendacionesConfig {

    @Bean
    public PoliticaReintentos politicaReintentos(SagaRecomendacionesProperties propiedades) {
        return new PoliticaReintentos(propiedades.maxIntentos(), propiedades.esperaInicial(),
                propiedades.esperaMaxima(), propiedades.variacion(), propiedades.concesion(),
                () -> ThreadLocalRandom.current().nextDouble());
    }

    /** Solo con la saga habilitada: reenvío programado y token de servicio. */
    @Configuration
    @ConditionalOnProperty(prefix = "scoring.saga", name = "habilitada", havingValue = "true")
    @EnableScheduling
    @EnableConfigurationProperties(ServicioJwtProperties.class)
    static class SagaHabilitada {
    }
}
