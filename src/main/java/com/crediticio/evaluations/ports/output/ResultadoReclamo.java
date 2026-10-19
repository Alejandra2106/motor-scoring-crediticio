package com.crediticio.evaluations.ports.output;

import java.util.UUID;

/** Resultado de intentar reclamar el siguiente trabajo de la saga (HU14). */
public sealed interface ResultadoReclamo {

    /** Se reclamó el trabajo con una concesión nueva; {@code intentos} incluye este intento. */
    record Reclamado(long idEvaluacion, UUID token, int intentos) implements ResultadoReclamo {
    }

    /** El trabajo agotó sus intentos sin una concesión vigente y pasó a ERROR_PERMANENTE. */
    record Agotado(long idEvaluacion) implements ResultadoReclamo {
    }

    /** No hay trabajos pendientes listos para reintentar. */
    record Ninguno() implements ResultadoReclamo {
    }
}
