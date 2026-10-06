package com.crediticio.evaluations.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValorVariableResolverTest {

    @Test
    void debeResolverIngresosMensualesComoTextoPlano() {
        SolicitanteParaEvaluacion solicitante = solicitante(new BigDecimal("4000000"), new BigDecimal("1200000"),
                2, "BUENO", new BigDecimal("3.5"));

        assertThat(ValorVariableResolver.resolver(NombreVariableEvaluada.INGRESOS_MENSUALES, solicitante))
                .isEqualTo("4000000");
    }

    @Test
    void debeResolverNumeroMorasComoTexto() {
        SolicitanteParaEvaluacion solicitante = solicitante(new BigDecimal("4000000"), new BigDecimal("1200000"),
                2, "BUENO", new BigDecimal("3.5"));

        assertThat(ValorVariableResolver.resolver(NombreVariableEvaluada.NUMERO_MORAS, solicitante))
                .isEqualTo("2");
    }

    @Test
    void debeResolverHistorialCrediticio() {
        SolicitanteParaEvaluacion solicitante = solicitante(new BigDecimal("4000000"), new BigDecimal("1200000"),
                2, "REGULAR", new BigDecimal("3.5"));

        assertThat(ValorVariableResolver.resolver(NombreVariableEvaluada.HISTORIAL_CREDITICIO, solicitante))
                .isEqualTo("REGULAR");
    }

    @Test
    void debeResolverAntiguedadLaboralComoTextoPlano() {
        SolicitanteParaEvaluacion solicitante = solicitante(new BigDecimal("4000000"), new BigDecimal("1200000"),
                2, "BUENO", new BigDecimal("3.5"));

        assertThat(ValorVariableResolver.resolver(NombreVariableEvaluada.ANTIGUEDAD_LABORAL, solicitante))
                .isEqualTo("3.5");
    }

    @Test
    void debeCalcularNivelEndeudamientoComoPorcentajeDeDeudasSobreIngresos() {
        
        SolicitanteParaEvaluacion solicitante = solicitante(new BigDecimal("4000000"), new BigDecimal("1200000"),
                0, "BUENO", new BigDecimal("1"));

        assertThat(ValorVariableResolver.resolver(NombreVariableEvaluada.NIVEL_ENDEUDAMIENTO, solicitante))
                .isEqualTo("30.0000000000");
    }

    @Test
    void debeLanzarNivelEndeudamientoIndeterminadoCuandoIngresosMensualesEsCero() {
        
        SolicitanteParaEvaluacion solicitante = solicitante(BigDecimal.ZERO, new BigDecimal("500000"),
                0, "BUENO", new BigDecimal("1"));

        assertThatThrownBy(() -> ValorVariableResolver.resolver(NombreVariableEvaluada.NIVEL_ENDEUDAMIENTO, solicitante))
                .isInstanceOf(NivelEndeudamientoIndeterminadoException.class);
    }

    @Test
    void debeLanzarExcepcionDefensivaCuandoFaltaUnDatoRequerido() {
        
        
        SolicitanteParaEvaluacion solicitante = solicitante(null, new BigDecimal("500000"),
                0, "BUENO", new BigDecimal("1"));

        assertThatThrownBy(() -> ValorVariableResolver.resolver(NombreVariableEvaluada.INGRESOS_MENSUALES, solicitante))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SolicitanteParaEvaluacion solicitante(BigDecimal ingresosMensuales, BigDecimal deudasMensuales,
            Integer numeroMoras, String historialCrediticio, BigDecimal antiguedadLaboral) {
        return new SolicitanteParaEvaluacion(1L, ingresosMensuales, deudasMensuales, numeroMoras,
                historialCrediticio, antiguedadLaboral);
    }
}
