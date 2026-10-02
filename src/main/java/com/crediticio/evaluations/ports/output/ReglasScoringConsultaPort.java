package com.crediticio.evaluations.ports.output;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReglasScoringConsultaPort {

    



    Set<Long> idsConReglasVigentes(Set<Long> idsRiesgoActivos);

    




    List<ResultadoReglaEvaluada> evaluar(Map<Long, String> valorRealPorRiesgo);
}
