package com.crediticio.riskvariables.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableRiesgoTest {

    @Test
    void debeCrearVariableRiesgoConEstadoActivaYSinIdNiFecha() {
        VariableRiesgo variableRiesgo = VariableRiesgo.nueva(
                NombreVariableRiesgo.INGRESOS_MENSUALES,
                "Ingresos mensuales netos declarados por el solicitante");

        assertThat(variableRiesgo.getVariable()).isEqualTo(NombreVariableRiesgo.INGRESOS_MENSUALES);
        assertThat(variableRiesgo.getDescripcion()).isEqualTo("Ingresos mensuales netos declarados por el solicitante");
        assertThat(variableRiesgo.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(variableRiesgo.getIdVariableRiesgo()).isNull();
        assertThat(variableRiesgo.getFechaCreacion()).isNull();
    }

    @Test
    void debeRecortarLosEspaciosDeLaDescripcion() {
        VariableRiesgo variableRiesgo = VariableRiesgo.nueva(
                NombreVariableRiesgo.NUMERO_MORAS,
                "   Número de moras registradas en el historial   ");

        assertThat(variableRiesgo.getDescripcion()).isEqualTo("Número de moras registradas en el historial");
    }

    @Test
    void debeRechazarDescripcionNula() {
        assertThatThrownBy(() -> VariableRiesgo.nueva(NombreVariableRiesgo.INGRESOS_MENSUALES, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "", "corta"})
    void debeRechazarDescripcionEnBlancoOMuyCorta(String descripcionInvalida) {
        assertThatThrownBy(() -> VariableRiesgo.nueva(NombreVariableRiesgo.INGRESOS_MENSUALES, descripcionInvalida))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarDescripcionMayorA255Caracteres() {
        String descripcionMuyLarga = "A".repeat(256);
        assertThatThrownBy(() -> VariableRiesgo.nueva(NombreVariableRiesgo.INGRESOS_MENSUALES, descripcionMuyLarga))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeAceptarDescripcionEnLosLimitesDeLongitud() {
        String descripcionDeDiezCaracteres = "1234567890";
        String descripcionDeDoscientosCincuentaYCinco = "A".repeat(255);

        assertThat(VariableRiesgo.nueva(NombreVariableRiesgo.INGRESOS_MENSUALES, descripcionDeDiezCaracteres)
                .getDescripcion()).hasSize(10);
        assertThat(VariableRiesgo.nueva(NombreVariableRiesgo.INGRESOS_MENSUALES, descripcionDeDoscientosCincuentaYCinco)
                .getDescripcion()).hasSize(255);
    }

    @Test
    void debeRechazarVariableNula() {
        assertThatThrownBy(() -> VariableRiesgo.nueva(null, "Descripción válida con más de diez caracteres"))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(NombreVariableRiesgo.class)
    void elTipoDebeDerivarseDeLaVariableSeleccionada(NombreVariableRiesgo variable) {
        VariableRiesgo variableRiesgo = VariableRiesgo.nueva(variable, "Descripción válida con más de diez caracteres");

        assertThat(variableRiesgo.getTipo()).isEqualTo(variable.getTipo());
    }

    @Test
    void lasVariablesNumericasDebenTenerTipoNumerico() {
        assertThat(NombreVariableRiesgo.INGRESOS_MENSUALES.getTipo()).isEqualTo(TipoVariableRiesgo.NUMERICO);
        assertThat(NombreVariableRiesgo.NIVEL_ENDEUDAMIENTO.getTipo()).isEqualTo(TipoVariableRiesgo.NUMERICO);
        assertThat(NombreVariableRiesgo.NUMERO_MORAS.getTipo()).isEqualTo(TipoVariableRiesgo.NUMERICO);
        assertThat(NombreVariableRiesgo.ANTIGUEDAD_LABORAL.getTipo()).isEqualTo(TipoVariableRiesgo.NUMERICO);
    }

    @Test
    void laVariableHistorialCrediticioDebeTenerTipoCategorico() {
        assertThat(NombreVariableRiesgo.HISTORIAL_CREDITICIO.getTipo()).isEqualTo(TipoVariableRiesgo.CATEGORICO);
    }

    @Test
    void cambiarEstadoDeActivaAInactivaDebePreservarLosDemasCampos() {
        VariableRiesgo variableRiesgo = VariableRiesgo.reconstruir(
                1L, NombreVariableRiesgo.INGRESOS_MENSUALES, "Ingresos mensuales netos declarados por el solicitante",
                EstadoVariableRiesgo.ACTIVA, java.time.LocalDateTime.of(2026, 1, 1, 10, 0));

        VariableRiesgo actualizada = variableRiesgo.cambiarEstado(EstadoVariableRiesgo.INACTIVA);

        assertThat(actualizada.getEstado()).isEqualTo(EstadoVariableRiesgo.INACTIVA);
        assertThat(actualizada.getIdVariableRiesgo()).isEqualTo(variableRiesgo.getIdVariableRiesgo());
        assertThat(actualizada.getVariable()).isEqualTo(variableRiesgo.getVariable());
        assertThat(actualizada.getDescripcion()).isEqualTo(variableRiesgo.getDescripcion());
        assertThat(actualizada.getFechaCreacion()).isEqualTo(variableRiesgo.getFechaCreacion());
    }

    @Test
    void cambiarEstadoDeInactivaAActivaDebePreservarLosDemasCampos() {
        VariableRiesgo variableRiesgo = VariableRiesgo.reconstruir(
                2L, NombreVariableRiesgo.NUMERO_MORAS, "Número de moras registradas en el historial crediticio",
                EstadoVariableRiesgo.INACTIVA, java.time.LocalDateTime.of(2026, 1, 1, 10, 0));

        VariableRiesgo actualizada = variableRiesgo.cambiarEstado(EstadoVariableRiesgo.ACTIVA);

        assertThat(actualizada.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(actualizada.getIdVariableRiesgo()).isEqualTo(variableRiesgo.getIdVariableRiesgo());
        assertThat(actualizada.getVariable()).isEqualTo(variableRiesgo.getVariable());
        assertThat(actualizada.getDescripcion()).isEqualTo(variableRiesgo.getDescripcion());
        assertThat(actualizada.getFechaCreacion()).isEqualTo(variableRiesgo.getFechaCreacion());
    }

    @Test
    void cambiarEstadoDebeRechazarEstadoNulo() {
        VariableRiesgo variableRiesgo = VariableRiesgo.nueva(
                NombreVariableRiesgo.INGRESOS_MENSUALES, "Ingresos mensuales netos declarados por el solicitante");

        assertThatThrownBy(() -> variableRiesgo.cambiarEstado(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reconstruirDebePreservarTodosLosCampos() {
        java.time.LocalDateTime fechaCreacion = java.time.LocalDateTime.of(2026, 1, 1, 10, 0);

        VariableRiesgo variableRiesgo = VariableRiesgo.reconstruir(
                1L, NombreVariableRiesgo.HISTORIAL_CREDITICIO, "Historial crediticio del solicitante evaluado",
                EstadoVariableRiesgo.ACTIVA, fechaCreacion);

        assertThat(variableRiesgo.getIdVariableRiesgo()).isEqualTo(1L);
        assertThat(variableRiesgo.getVariable()).isEqualTo(NombreVariableRiesgo.HISTORIAL_CREDITICIO);
        assertThat(variableRiesgo.getDescripcion()).isEqualTo("Historial crediticio del solicitante evaluado");
        assertThat(variableRiesgo.getEstado()).isEqualTo(EstadoVariableRiesgo.ACTIVA);
        assertThat(variableRiesgo.getFechaCreacion()).isEqualTo(fechaCreacion);
    }
}
