package com.crediticio.evaluations.domain;

import java.math.BigDecimal;

/**
 * Proyección mínima de un solicitante necesaria para calcular su score, en la representación
 * propia de {@code evaluations}. La fuente de verdad sigue siendo el módulo {@code applicants};
 * esta proyección existe para que el dominio de {@code evaluations} no dependa de las clases de
 * dominio de ese módulo.
 */
public record SolicitanteParaEvaluacion(
        Long idSolicitante,
        BigDecimal ingresosMensuales,
        BigDecimal deudasMensuales,
        Integer numeroMoras,
        String historialCrediticio,
        BigDecimal antiguedadLaboral) {
}
