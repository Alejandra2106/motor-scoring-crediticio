package com.crediticio.scoring.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperadorScoringTest {

    @Test
    void desdeSimboloDebeResolverLosCincoOperadoresPermitidos() {
        assertThat(OperadorScoring.desdeSimbolo("=")).isEqualTo(OperadorScoring.IGUAL);
        assertThat(OperadorScoring.desdeSimbolo(">")).isEqualTo(OperadorScoring.MAYOR);
        assertThat(OperadorScoring.desdeSimbolo(">=")).isEqualTo(OperadorScoring.MAYOR_O_IGUAL);
        assertThat(OperadorScoring.desdeSimbolo("<")).isEqualTo(OperadorScoring.MENOR);
        assertThat(OperadorScoring.desdeSimbolo("<=")).isEqualTo(OperadorScoring.MENOR_O_IGUAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"==", "!=", "menor", ""})
    void desdeSimboloDebeRechazarSimbolosFueraDelConjuntoPermitido(String simboloInvalido) {
        assertThatThrownBy(() -> OperadorScoring.desdeSimbolo(simboloInvalido))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSimboloDebeRetornarElCaracterOriginal() {
        assertThat(OperadorScoring.MAYOR_O_IGUAL.getSimbolo()).isEqualTo(">=");
    }

    @Test
    void esSoloIgualdadDebeSerVerdaderoUnicamenteParaIgual() {
        assertThat(OperadorScoring.IGUAL.esSoloIgualdad()).isTrue();
        assertThat(OperadorScoring.MAYOR.esSoloIgualdad()).isFalse();
        assertThat(OperadorScoring.MAYOR_O_IGUAL.esSoloIgualdad()).isFalse();
        assertThat(OperadorScoring.MENOR.esSoloIgualdad()).isFalse();
        assertThat(OperadorScoring.MENOR_O_IGUAL.esSoloIgualdad()).isFalse();
    }
}
