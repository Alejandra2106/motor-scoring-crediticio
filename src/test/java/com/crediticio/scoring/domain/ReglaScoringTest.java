package com.crediticio.scoring.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglaScoringTest {

    @Test
    void debeCrearReglaNumericaConEstadoActivaYSinIdNiFecha() {
        ReglaScoring regla = ReglaScoring.nueva(1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20,
                TipoVariable.NUMERICO, true);

        assertThat(regla.getIdRegla()).isNull();
        assertThat(regla.getIdRiesgo()).isEqualTo(1L);
        assertThat(regla.getOperador()).isEqualTo(OperadorScoring.MAYOR_O_IGUAL);
        assertThat(regla.getValorCondicion()).isEqualTo("3000000");
        assertThat(regla.getPuntaje()).isEqualTo(20);
        assertThat(regla.getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
        assertThat(regla.getFechaCreacion()).isNull();
    }

    @Test
    void debeRecortarLosEspaciosDelValorCondicion() {
        ReglaScoring regla = ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "  3000000  ", 20,
                TipoVariable.NUMERICO, true);

        assertThat(regla.getValorCondicion()).isEqualTo("3000000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void debeRechazarValorCondicionVacioOEnBlanco(String valorInvalido) {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, OperadorScoring.IGUAL, valorInvalido, 20,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarValorCondicionNulo() {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, OperadorScoring.IGUAL, null, 20,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarIdRiesgoNulo() {
        assertThatThrownBy(() -> ReglaScoring.nueva(null, OperadorScoring.IGUAL, "3000000", 20,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarOperadorNulo() {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, null, "3000000", 20,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAYOR", "MAYOR_O_IGUAL", "MENOR", "MENOR_O_IGUAL"})
    void debeRechazarOperadorDistintoDeIgualParaVariableCategorica(String nombreOperador) {
        OperadorScoring operador = OperadorScoring.valueOf(nombreOperador);

        assertThatThrownBy(() -> ReglaScoring.nueva(4L, operador, "BUENO", 30, TipoVariable.CATEGORICO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeAceptarOperadorIgualParaVariableCategorica() {
        ReglaScoring regla = ReglaScoring.nueva(4L, OperadorScoring.IGUAL, "BUENO", 30, TipoVariable.CATEGORICO, false);

        assertThat(regla.getOperador()).isEqualTo(OperadorScoring.IGUAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"=", ">", ">=", "<", "<="})
    void debeAceptarLosCincoOperadoresParaVariableNumerica(String simbolo) {
        OperadorScoring operador = OperadorScoring.desdeSimbolo(simbolo);

        ReglaScoring regla = ReglaScoring.nueva(1L, operador, "10", 5, TipoVariable.NUMERICO, true);

        assertThat(regla.getOperador()).isEqualTo(operador);
    }

    @Test
    void debeRechazarValorCondicionNoNumericoParaVariableNumerica() {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "ABC", 20,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarValorCondicionNegativoParaVariableNumerica() {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "-100", 20,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeAceptarValorCondicionCero() {
        ReglaScoring regla = ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "0", 20, TipoVariable.NUMERICO, true);

        assertThat(regla.getValorCondicion()).isEqualTo("0");
    }

    @Test
    void debeAceptarValorDecimalCuandoLaVariablePermiteDecimales() {
        ReglaScoring regla = ReglaScoring.nueva(2L, OperadorScoring.MENOR_O_IGUAL, "0.40", 10,
                TipoVariable.NUMERICO, true);

        assertThat(regla.getValorCondicion()).isEqualTo("0.40");
    }

    @Test
    void debeRechazarValorDecimalCuandoLaVariableNoPermiteDecimales() {
        assertThatThrownBy(() -> ReglaScoring.nueva(3L, OperadorScoring.IGUAL, "2.5", 10,
                TipoVariable.NUMERICO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeAceptarValorEnteroCuandoLaVariableNoPermiteDecimales() {
        ReglaScoring regla = ReglaScoring.nueva(3L, OperadorScoring.MAYOR_O_IGUAL, "3", 10,
                TipoVariable.NUMERICO, false);

        assertThat(regla.getValorCondicion()).isEqualTo("3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BUENO", "REGULAR", "MALO"})
    void debeAceptarLosValoresPermitidosDeHistorialCrediticio(String valorPermitido) {
        ReglaScoring regla = ReglaScoring.nueva(4L, OperadorScoring.IGUAL, valorPermitido, 30,
                TipoVariable.CATEGORICO, false);

        assertThat(regla.getValorCondicion()).isEqualTo(valorPermitido);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXCELENTE", "MUY_BUENO", "MALO_REGULAR", "bueno"})
    void debeRechazarValoresCategoricosFueraDelCatalogoPermitido(String valorInvalido) {
        assertThatThrownBy(() -> ReglaScoring.nueva(4L, OperadorScoring.IGUAL, valorInvalido, 30,
                TipoVariable.CATEGORICO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarPuntajeNulo() {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "10", null,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-100, -50, 0, 75, 100})
    void debeAceptarPuntajeDentroDelRangoPermitido(int puntajeValido) {
        ReglaScoring regla = ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "10", puntajeValido,
                TipoVariable.NUMERICO, true);

        assertThat(regla.getPuntaje()).isEqualTo(puntajeValido);
    }

    @ParameterizedTest
    @ValueSource(ints = {-101, 101})
    void debeRechazarPuntajeFueraDelRangoPermitido(int puntajeInvalido) {
        assertThatThrownBy(() -> ReglaScoring.nueva(1L, OperadorScoring.IGUAL, "10", puntajeInvalido,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstruirDebePreservarTodosLosCampos() {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 1, 1, 10, 0);

        ReglaScoring regla = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, fechaCreacion);

        assertThat(regla.getIdRegla()).isEqualTo(7L);
        assertThat(regla.getIdRiesgo()).isEqualTo(1L);
        assertThat(regla.getOperador()).isEqualTo(OperadorScoring.MAYOR_O_IGUAL);
        assertThat(regla.getValorCondicion()).isEqualTo("3000000");
        assertThat(regla.getPuntaje()).isEqualTo(20);
        assertThat(regla.getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
        assertThat(regla.getFechaCreacion()).isEqualTo(fechaCreacion);
    }
}
