package com.crediticio.evaluations.domain;

import java.math.BigDecimal;







public record SolicitanteParaEvaluacion(
        Long idSolicitante,
        BigDecimal ingresosMensuales,
        BigDecimal deudasMensuales,
        Integer numeroMoras,
        String historialCrediticio,
        BigDecimal antiguedadLaboral) {
}
