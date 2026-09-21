package com.crediticio.evaluations.ports.output;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReglasScoringConsultaPort {

    /**
     * Entre las variables de riesgo activas recibidas (por {@code idRiesgo}), retorna aquellas
     * que tienen al menos una regla de scoring ACTIVA asociada (RF04/RF08).
     */
    Set<Long> idsConReglasVigentes(Set<Long> idsRiesgoActivos);

    /**
     * Evalúa todas las reglas ACTIVAS asociadas a los {@code idRiesgo} recibidos, usando el
     * valor real correspondiente a cada uno (RF06/RF07). El mapa debe contener únicamente
     * variables que efectivamente tienen reglas asociadas (ver {@link #idsConReglasVigentes}).
     */
    List<ResultadoReglaEvaluada> evaluar(Map<Long, String> valorRealPorRiesgo);
}
