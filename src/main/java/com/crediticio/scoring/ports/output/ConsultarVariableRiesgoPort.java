package com.crediticio.scoring.ports.output;

import java.util.Optional;

public interface ConsultarVariableRiesgoPort {

    Optional<VariableRiesgoConsultada> consultar(Long idRiesgo);
}
