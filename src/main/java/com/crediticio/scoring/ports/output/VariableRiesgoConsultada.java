package com.crediticio.scoring.ports.output;

import com.crediticio.scoring.domain.TipoVariable;

public record VariableRiesgoConsultada(boolean activa, TipoVariable tipo, boolean permiteDecimales) {
}
