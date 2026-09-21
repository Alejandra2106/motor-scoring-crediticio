package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.ports.output.ResultadoReglaEvaluada;
import com.crediticio.scoring.domain.EstadoReglaScoring;
import com.crediticio.scoring.domain.OperadorScoring;
import com.crediticio.scoring.domain.ReglaScoring;
import com.crediticio.scoring.domain.TipoVariable;
import com.crediticio.scoring.ports.output.ConsultarVariableRiesgoPort;
import com.crediticio.scoring.ports.output.ReglaScoringRepositoryPort;
import com.crediticio.scoring.ports.output.VariableRiesgoConsultada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReglasScoringConsultaAdapterTest {

    @Mock
    private ReglaScoringRepositoryPort reglaScoringRepositoryPort;

    @Mock
    private ConsultarVariableRiesgoPort consultarVariableRiesgoPort;

    private ReglasScoringConsultaAdapter adapter;

    @Test
    void idsConReglasVigentesDebeRetornarSoloLosRiesgosConAlMenosUnaReglaActiva() {
        adapter = new ReglasScoringConsultaAdapter(reglaScoringRepositoryPort, consultarVariableRiesgoPort);
        when(reglaScoringRepositoryPort.listarActivasPorRiesgos(Set.of(1L, 2L))).thenReturn(List.of(
                ReglaScoring.reconstruir(1L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20,
                        EstadoReglaScoring.ACTIVA, null)));

        Set<Long> resultado = adapter.idsConReglasVigentes(Set.of(1L, 2L));

        assertThat(resultado).containsExactly(1L);
    }

    @Test
    void evaluarDebeMarcarCondicionCumplidaYAsignarElPuntajeDeLaRegla() {
        adapter = new ReglasScoringConsultaAdapter(reglaScoringRepositoryPort, consultarVariableRiesgoPort);
        when(reglaScoringRepositoryPort.listarActivasPorRiesgos(Set.of(1L))).thenReturn(List.of(
                ReglaScoring.reconstruir(10L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20,
                        EstadoReglaScoring.ACTIVA, null)));
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));

        List<ResultadoReglaEvaluada> resultados = adapter.evaluar(Map.of(1L, "5000000"));

        assertThat(resultados).hasSize(1);
        ResultadoReglaEvaluada resultado = resultados.get(0);
        assertThat(resultado.idRegla()).isEqualTo(10L);
        assertThat(resultado.operadorAplicado()).isEqualTo(">=");
        assertThat(resultado.valorCondicionAplicado()).isEqualTo("3000000");
        assertThat(resultado.puntajeReglaAplicado()).isEqualTo(20);
        assertThat(resultado.condicionCumplida()).isTrue();
        assertThat(resultado.puntajeObtenido()).isEqualTo(20);
    }

    @Test
    void evaluarDebeAsignarPuntajeObtenidoEnCeroCuandoLaCondicionNoSeCumple() {
        adapter = new ReglasScoringConsultaAdapter(reglaScoringRepositoryPort, consultarVariableRiesgoPort);
        when(reglaScoringRepositoryPort.listarActivasPorRiesgos(Set.of(1L))).thenReturn(List.of(
                ReglaScoring.reconstruir(10L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20,
                        EstadoReglaScoring.ACTIVA, null)));
        when(consultarVariableRiesgoPort.consultar(1L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.NUMERICO, true)));

        List<ResultadoReglaEvaluada> resultados = adapter.evaluar(Map.of(1L, "1000000"));

        ResultadoReglaEvaluada resultado = resultados.get(0);
        assertThat(resultado.condicionCumplida()).isFalse();
        assertThat(resultado.puntajeObtenido()).isZero();
    }

    @Test
    void evaluarDebeEvaluarCorrectamenteUnaReglaCategorica() {
        adapter = new ReglasScoringConsultaAdapter(reglaScoringRepositoryPort, consultarVariableRiesgoPort);
        when(reglaScoringRepositoryPort.listarActivasPorRiesgos(Set.of(4L))).thenReturn(List.of(
                ReglaScoring.reconstruir(20L, 4L, OperadorScoring.IGUAL, "BUENO", 30,
                        EstadoReglaScoring.ACTIVA, null)));
        when(consultarVariableRiesgoPort.consultar(4L))
                .thenReturn(Optional.of(new VariableRiesgoConsultada(true, TipoVariable.CATEGORICO, false)));

        List<ResultadoReglaEvaluada> resultados = adapter.evaluar(Map.of(4L, "BUENO"));

        assertThat(resultados.get(0).condicionCumplida()).isTrue();
    }
}
