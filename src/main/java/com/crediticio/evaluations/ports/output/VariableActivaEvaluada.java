package com.crediticio.evaluations.ports.output;

import com.crediticio.evaluations.domain.NombreVariableEvaluada;

public record VariableActivaEvaluada(Long idRiesgo, NombreVariableEvaluada variable) {
}
