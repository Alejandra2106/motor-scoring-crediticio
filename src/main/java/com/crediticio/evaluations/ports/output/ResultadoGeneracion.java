package com.crediticio.evaluations.ports.output;

/** Resultado de pedir a evaluation-query-api que genere las recomendaciones de una evaluación (HU14). */
public sealed interface ResultadoGeneracion {

    /** Recomendaciones persistidas (nuevas o ya existentes por un intento anterior). */
    record Exito(String versionCatalogo, int cantidad) implements ResultadoGeneracion {
    }

    /** Fallo que puede resolverse reintentando (conexión, tiempo agotado, 5xx, catálogo no disponible). */
    record ErrorTransitorio(String codigo, String detalle) implements ResultadoGeneracion {
    }

    /** Fallo que no se resuelve reintentando (rechazo funcional, identidad rechazada, contrato inválido). */
    record ErrorPermanente(String codigo, String detalle) implements ResultadoGeneracion {
    }

    /** La saga no está habilitada en este despliegue: no se intenta ni se registra nada. */
    record NoDisponible() implements ResultadoGeneracion {
    }
}
