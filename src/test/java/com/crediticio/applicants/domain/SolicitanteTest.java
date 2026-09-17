package com.crediticio.applicants.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitanteTest {

    @Test
    void debeCrearSolicitanteConDatosValidos() {
        Solicitante solicitante = Solicitante.nuevo(
                "Ana María Pérez",
                "1234567",
                new BigDecimal("3000000"),
                new BigDecimal("500000"),
                0,
                HistorialCrediticio.BUENO,
                new BigDecimal("2.5"));

        assertThat(solicitante.getNombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(solicitante.getNumeroDocumento()).isEqualTo("1234567");
        assertThat(solicitante.getIngresosMensuales()).isEqualTo(new BigDecimal("3000000"));
        assertThat(solicitante.getDeudasMensuales()).isEqualTo(new BigDecimal("500000"));
        assertThat(solicitante.getNumeroMoras()).isZero();
        assertThat(solicitante.getHistorialCrediticio()).isEqualTo(HistorialCrediticio.BUENO);
        assertThat(solicitante.getAntiguedadLaboral()).isEqualTo(new BigDecimal("2.5"));
        assertThat(solicitante.getIdSolicitante()).isNull();
        assertThat(solicitante.getFechaRegistro()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"An", "  ", ""})
    void debeRechazarNombreInvalido(String nombreInvalido) {
        assertThatThrownBy(() -> solicitanteValido(nombreInvalido, "1234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarNombreNulo() {
        assertThatThrownBy(() -> solicitanteValido(null, "1234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarNombreMayorA100Caracteres() {
        String nombreMuyLargo = "A".repeat(101);
        assertThatThrownBy(() -> solicitanteValido(nombreMuyLargo, "1234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567890123456", "12345a", "abcdef"})
    void debeRechazarDocumentoInvalido(String documentoInvalido) {
        assertThatThrownBy(() -> solicitanteValido("Nombre Válido", documentoInvalido))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarIngresosNegativos() {
        assertThatThrownBy(() -> Solicitante.nuevo(
                "Nombre Válido", "1234567", new BigDecimal("-1"), BigDecimal.ZERO, 0,
                HistorialCrediticio.BUENO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarDeudasNegativas() {
        assertThatThrownBy(() -> Solicitante.nuevo(
                "Nombre Válido", "1234567", BigDecimal.ZERO, new BigDecimal("-1"), 0,
                HistorialCrediticio.BUENO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarNumeroDeMorasNegativo() {
        assertThatThrownBy(() -> Solicitante.nuevo(
                "Nombre Válido", "1234567", BigDecimal.ZERO, BigDecimal.ZERO, -1,
                HistorialCrediticio.BUENO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarHistorialCrediticioNulo() {
        assertThatThrownBy(() -> Solicitante.nuevo(
                "Nombre Válido", "1234567", BigDecimal.ZERO, BigDecimal.ZERO, 0, null, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarAntiguedadLaboralNegativa() {
        assertThatThrownBy(() -> Solicitante.nuevo(
                "Nombre Válido", "1234567", BigDecimal.ZERO, BigDecimal.ZERO, 0,
                HistorialCrediticio.BUENO, new BigDecimal("-0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Solicitante solicitanteValido(String nombreCompleto, String numeroDocumento) {
        return Solicitante.nuevo(
                nombreCompleto,
                numeroDocumento,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                HistorialCrediticio.BUENO,
                BigDecimal.ZERO);
    }
}
