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
    void editarDebeActualizarOperadorValorCondicionYPuntajePreservandoIdReglaIdRiesgoEstadoYFechaCreacion() {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 1, 1, 10, 0);
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, fechaCreacion);

        ReglaScoring editada = reglaExistente.editar(OperadorScoring.MENOR, "5000000", 25, TipoVariable.NUMERICO, true);

        assertThat(editada.getIdRegla()).isEqualTo(7L);
        assertThat(editada.getIdRiesgo()).isEqualTo(1L);
        assertThat(editada.getOperador()).isEqualTo(OperadorScoring.MENOR);
        assertThat(editada.getValorCondicion()).isEqualTo("5000000");
        assertThat(editada.getPuntaje()).isEqualTo(25);
        assertThat(editada.getEstado()).isEqualTo(EstadoReglaScoring.ACTIVA);
        assertThat(editada.getFechaCreacion()).isEqualTo(fechaCreacion);
    }

    @Test
    void editarDebePermitirConservarLaMismaCombinacionOriginal() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                7L, 1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20, EstadoReglaScoring.ACTIVA, null);

        ReglaScoring editada = reglaExistente.editar(OperadorScoring.MAYOR_O_IGUAL, "3000000", 20,
                TipoVariable.NUMERICO, true);

        assertThat(editada.getOperador()).isEqualTo(OperadorScoring.MAYOR_O_IGUAL);
        assertThat(editada.getValorCondicion()).isEqualTo("3000000");
    }

    @Test
    void editarDebeRechazarOperadorIncompatibleConVariableCategorica() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                4L, 4L, OperadorScoring.IGUAL, "BUENO", 30, EstadoReglaScoring.ACTIVA, null);

        assertThatThrownBy(() -> reglaExistente.editar(OperadorScoring.MAYOR, "BUENO", 30, TipoVariable.CATEGORICO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editarDebeRechazarValorCondicionNoNumericoParaVariableNumerica() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.IGUAL, "10", 20, EstadoReglaScoring.ACTIVA, null);

        assertThatThrownBy(() -> reglaExistente.editar(OperadorScoring.IGUAL, "ABC", 20, TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editarDebeRechazarValorCategoricoFueraDelCatalogoPermitido() {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                4L, 4L, OperadorScoring.IGUAL, "BUENO", 30, EstadoReglaScoring.ACTIVA, null);

        assertThatThrownBy(() -> reglaExistente.editar(OperadorScoring.IGUAL, "EXCELENTE", 30,
                TipoVariable.CATEGORICO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-101, 101})
    void editarDebeRechazarPuntajeFueraDelRangoPermitido(int puntajeInvalido) {
        ReglaScoring reglaExistente = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.IGUAL, "10", 20, EstadoReglaScoring.ACTIVA, null);

        assertThatThrownBy(() -> reglaExistente.editar(OperadorScoring.IGUAL, "10", puntajeInvalido,
                TipoVariable.NUMERICO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluarIgualDebeCumplirseCuandoElValorRealCoincideConLaCondicion() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.IGUAL, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("5000000", TipoVariable.NUMERICO)).isTrue();
    }

    @Test
    void evaluarMayorDebeCumplirseCuandoElValorRealSuperaLaCondicion() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.MAYOR, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("6000000", TipoVariable.NUMERICO)).isTrue();
        assertThat(regla.evaluar("5000000", TipoVariable.NUMERICO)).isFalse();
    }

    @Test
    void evaluarMayorOIgualDebeCumplirseParaValorRealIgualOMayor() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.MAYOR_O_IGUAL, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("5000000", TipoVariable.NUMERICO)).isTrue();
        assertThat(regla.evaluar("4999999", TipoVariable.NUMERICO)).isFalse();
    }

    @Test
    void evaluarMenorDebeCumplirseCuandoElValorRealEsMenorQueLaCondicion() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.MENOR, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("4000000", TipoVariable.NUMERICO)).isTrue();
        assertThat(regla.evaluar("5000000", TipoVariable.NUMERICO)).isFalse();
    }

    @Test
    void evaluarMenorOIgualDebeCumplirseParaValorRealIgualOMenor() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.MENOR_O_IGUAL, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("5000000", TipoVariable.NUMERICO)).isTrue();
        assertThat(regla.evaluar("5000001", TipoVariable.NUMERICO)).isFalse();
    }

    @Test
    void evaluarDebeSerFalsoCuandoLaCondicionNumericaNoSeCumple() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.MAYOR_O_IGUAL, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("4999999", TipoVariable.NUMERICO)).isFalse();
    }

    @Test
    void evaluarDebeCompararNumericamenteSinImportarElFormatoDelTexto() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.IGUAL, "5000000", 20, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("5000000.00", TipoVariable.NUMERICO)).isTrue();
    }

    @Test
    void evaluarDebeCumplirseParaVariableCategoricaCuandoElValorRealCoincide() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                4L, 4L, OperadorScoring.IGUAL, "BUENO", 30, EstadoReglaScoring.ACTIVA, null);

        assertThat(regla.evaluar("BUENO", TipoVariable.CATEGORICO)).isTrue();
        assertThat(regla.evaluar("REGULAR", TipoVariable.CATEGORICO)).isFalse();
    }

    @Test
    void evaluarDebeRechazarValorRealNulo() {
        ReglaScoring regla = ReglaScoring.reconstruir(
                1L, 1L, OperadorScoring.IGUAL, "10", 20, EstadoReglaScoring.ACTIVA, null);

        assertThatThrownBy(() -> regla.evaluar(null, TipoVariable.NUMERICO))
                .isInstanceOf(NullPointerException.class);
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
