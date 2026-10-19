package com.crediticio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Saga de recomendaciones de HU14 (valores aprobados el 2026-10-18). Deshabilitada por defecto: sin ella, las
 * evaluaciones quedan PENDIENTE (V11) y no se presentan como completas. Solo debe habilitarse con
 * evaluation-query-api desplegada, la identidad de servicio configurada y el catálogo APROBADO.
 *
 * @param urlBase           URL base de evaluation-query-api (obligatoria si está habilitada)
 * @param maxIntentos       intentos por ciclo, incluido el intento en línea
 * @param conexionEnLinea   tiempo máximo de conexión del intento en línea
 * @param limiteEnLinea     límite total de la respuesta del intento en línea
 * @param conexionReenvio   tiempo máximo de conexión del proceso de reenvío
 * @param limiteReenvio     límite total de la respuesta del proceso de reenvío
 * @param trabajosPorCiclo  trabajos que el proceso de reenvío reclama, de uno en uno, en cada ejecución
 */
@ConfigurationProperties("scoring.saga")
public record SagaRecomendacionesProperties(
        boolean habilitada,
        String urlBase,
        int maxIntentos,
        Duration esperaInicial,
        Duration esperaMaxima,
        double variacion,
        Duration concesion,
        Duration conexionEnLinea,
        Duration limiteEnLinea,
        Duration conexionReenvio,
        Duration limiteReenvio,
        int trabajosPorCiclo) {
}
