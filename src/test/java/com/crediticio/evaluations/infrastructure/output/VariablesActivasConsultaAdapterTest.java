package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.evaluations.domain.NombreVariableEvaluada;
import com.crediticio.evaluations.ports.output.VariableActivaEvaluada;
import com.crediticio.riskvariables.domain.EstadoVariableRiesgo;
import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
import com.crediticio.riskvariables.domain.VariableRiesgo;
import com.crediticio.riskvariables.ports.output.VariableRiesgoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariablesActivasConsultaAdapterTest {

    @Mock
    private VariableRiesgoRepositoryPort variableRiesgoRepositoryPort;

    private VariablesActivasConsultaAdapter adapter;

    @Test
    void debeRetornarListaVaciaCuandoNoHayVariablesActivas() {
        adapter = new VariablesActivasConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.listarActivas()).thenReturn(List.of());

        assertThat(adapter.listarActivas()).isEmpty();
    }

    @Test
    void debeMapearCadaVariableActivaConSuIdRiesgoYNombre() {
        adapter = new VariablesActivasConsultaAdapter(variableRiesgoRepositoryPort);
        when(variableRiesgoRepositoryPort.listarActivas()).thenReturn(List.of(
                VariableRiesgo.reconstruir(1L, NombreVariableRiesgo.INGRESOS_MENSUALES,
                        "Ingresos mensuales netos declarados", EstadoVariableRiesgo.ACTIVA, null),
                VariableRiesgo.reconstruir(2L, NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO,
                        "Porcentaje de deudas sobre ingresos", EstadoVariableRiesgo.ACTIVA, null)));

        List<VariableActivaEvaluada> variables = adapter.listarActivas();

        assertThat(variables).containsExactlyInAnyOrder(
                new VariableActivaEvaluada(1L, NombreVariableEvaluada.INGRESOS_MENSUALES),
                new VariableActivaEvaluada(2L, NombreVariableEvaluada.NIVEL_ENDEUDAMIENTO));
    }
}
