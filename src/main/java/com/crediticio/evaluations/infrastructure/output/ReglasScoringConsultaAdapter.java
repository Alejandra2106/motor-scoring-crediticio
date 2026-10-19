package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.ReglasScoringConsultaPort;
import com.crediticio.evaluations.ports.output.ResultadoReglaEvaluada;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.ports.output.ConsultarVariableRiesgoPort;
import com.crediticio.scoring.ports.output.ReglaScoringRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Único componente de {@code evaluations} que conoce el modelo de dominio de {@code scoring}.
 * Traduce esa información hacia el contrato propio de {@code evaluations}
 * ({@link ReglasScoringConsultaPort}), reutilizando los puertos de salida ya existentes en
 * {@code scoring} ({@link ReglaScoringRepositoryPort} y {@link ConsultarVariableRiesgoPort},
 * este último ya usado por HU05/HU06) sin duplicar la semántica de operadores.
 */
@Component
public class ReglasScoringConsultaAdapter implements ReglasScoringConsultaPort {

    private final ReglaScoringRepositoryPort reglaScoringRepositoryPort;
    private final ConsultarVariableRiesgoPort consultarVariableRiesgoPort;

    public ReglasScoringConsultaAdapter(ReglaScoringRepositoryPort reglaScoringRepositoryPort,
            ConsultarVariableRiesgoPort consultarVariableRiesgoPort) {
        this.reglaScoringRepositoryPort = reglaScoringRepositoryPort;
        this.consultarVariableRiesgoPort = consultarVariableRiesgoPort;
    }

    @Override
    public Set<Long> idsConReglasVigentes(Set<Long> idsRiesgoActivos) {
        return reglaScoringRepositoryPort.listarActivasPorRiesgos(idsRiesgoActivos).stream()
                .map(ReglaScoring::getIdRiesgo)
                .collect(Collectors.toSet());
    }

    @Override
    public List<ResultadoReglaEvaluada> evaluar(Map<Long, String> valorRealPorRiesgo) {
        List<ReglaScoring> reglas = reglaScoringRepositoryPort.listarActivasPorRiesgos(valorRealPorRiesgo.keySet());
        return reglas.stream().map(regla -> evaluarRegla(regla, valorRealPorRiesgo)).toList();
    }

    private ResultadoReglaEvaluada evaluarRegla(ReglaScoring regla, Map<Long, String> valorRealPorRiesgo) {
        TipoVariable tipoVariable = consultarVariableRiesgoPort.consultar(regla.getIdRiesgo())
                .orElseThrow(IllegalStateException::new)
                .tipo();
        String valorReal = valorRealPorRiesgo.get(regla.getIdRiesgo());
        boolean condicionCumplida = regla.evaluar(valorReal, tipoVariable);
        int puntajeObtenido = condicionCumplida ? regla.getPuntaje() : 0;
        return new ResultadoReglaEvaluada(
                regla.getIdRegla(),
                regla.getOperador().getSimbolo(),
                regla.getValorCondicion(),
                regla.getPuntaje(),
                condicionCumplida,
                puntajeObtenido);
    }
}
