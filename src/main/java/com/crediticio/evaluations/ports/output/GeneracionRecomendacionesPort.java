package com.crediticio.evaluations.ports.output;

/**
 * Operación interna de evaluation-query-api que genera y persiste las recomendaciones de una evaluación
 * (HU14). Es idempotente: si ya se generaron, devuelve la generación existente.
 */
public interface GeneracionRecomendacionesPort {

    /** Modo del intento: determina el límite total de la llamada. */
    enum ModoIntento {
        EN_LINEA,
        REENVIO
    }

    ResultadoGeneracion generar(long idEvaluacion, ModoIntento modo);
}
