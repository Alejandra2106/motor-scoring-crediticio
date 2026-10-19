package com.crediticio.evaluations.ports.output;

import java.time.Duration;
import java.util.UUID;

/**
 * Outbox de la saga de recomendaciones (HU14, tabla trabajo_recomendacion de V11). Cada registro de resultado
 * exige el token de la concesión vigente: un trabajador desfasado no modifica nada y recibe {@code false}.
 */
public interface TrabajoRecomendacionPort {

    /** Crea el trabajo de una evaluación nueva, en la misma transacción, con la concesión del intento en línea. */
    void crear(long idEvaluacion, UUID token, Duration concesion);

    /**
     * Reclama el siguiente trabajo PENDIENTE cuyo próximo intento ya llegó y sin concesión vigente. Si ya agotó
     * {@code maxIntentos}, lo marca ERROR_PERMANENTE (CONCESIONES_AGOTADAS) en lugar de reclamarlo.
     */
    ResultadoReclamo reclamarSiguiente(int maxIntentos, UUID token, Duration concesion);

    /** Trabajo PROCESADO y evaluación COMPLETADA, en una transacción. */
    boolean registrarExito(long idEvaluacion, UUID token);

    boolean registrarErrorTransitorio(long idEvaluacion, UUID token, Duration espera, String codigo, String detalle);

    boolean registrarErrorPermanente(long idEvaluacion, UUID token, String codigo, String detalle);
}
