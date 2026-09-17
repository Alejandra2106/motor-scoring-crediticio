package com.crediticio.applicants.application.dto;

import com.crediticio.applicants.domain.HistorialCrediticio;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrarSolicitanteRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void inicializarValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void unaSolicitudValidaNoDebeGenerarViolaciones() {
        Set<ConstraintViolation<RegistrarSolicitanteRequest>> violaciones = validator.validate(solicitudValida());
        assertThat(violaciones).isEmpty();
    }

    @Test
    void debeRechazarNombreCompletoEnBlanco() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setNombreCompleto("   ");
        assertThat(violacionesEn(request, "nombreCompleto")).isNotEmpty();
    }

    @Test
    void debeRechazarNombreCompletoMuyCorto() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setNombreCompleto("An");
        assertThat(violacionesEn(request, "nombreCompleto")).isNotEmpty();
    }

    @Test
    void debeRechazarNumeroDocumentoConLetras() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setNumeroDocumento("12345a");
        assertThat(violacionesEn(request, "numeroDocumento")).isNotEmpty();
    }

    @Test
    void debeRechazarNumeroDocumentoMuyCorto() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setNumeroDocumento("12345");
        assertThat(violacionesEn(request, "numeroDocumento")).isNotEmpty();
    }

    @Test
    void debeRechazarIngresosMensualesNegativos() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setIngresosMensuales(new BigDecimal("-1"));
        assertThat(violacionesEn(request, "ingresosMensuales")).isNotEmpty();
    }

    @Test
    void debeRechazarIngresosMensualesNulos() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setIngresosMensuales(null);
        assertThat(violacionesEn(request, "ingresosMensuales")).isNotEmpty();
    }

    @Test
    void debeRechazarDeudasMensualesNegativas() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setDeudasMensuales(new BigDecimal("-1"));
        assertThat(violacionesEn(request, "deudasMensuales")).isNotEmpty();
    }

    @Test
    void debeRechazarNumeroMorasNegativo() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setNumeroMoras(-1);
        assertThat(violacionesEn(request, "numeroMoras")).isNotEmpty();
    }

    @Test
    void debeRechazarHistorialCrediticioNulo() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setHistorialCrediticio(null);
        assertThat(violacionesEn(request, "historialCrediticio")).isNotEmpty();
    }

    @Test
    void debeRechazarAntiguedadLaboralNegativa() {
        RegistrarSolicitanteRequest request = solicitudValida();
        request.setAntiguedadLaboral(new BigDecimal("-0.5"));
        assertThat(violacionesEn(request, "antiguedadLaboral")).isNotEmpty();
    }

    private Set<ConstraintViolation<RegistrarSolicitanteRequest>> violacionesEn(RegistrarSolicitanteRequest request,
            String propiedad) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(propiedad))
                .collect(java.util.stream.Collectors.toSet());
    }

    private RegistrarSolicitanteRequest solicitudValida() {
        return new RegistrarSolicitanteRequest(
                "Ana María Pérez",
                "1234567",
                new BigDecimal("3000000"),
                new BigDecimal("500000"),
                0,
                HistorialCrediticio.BUENO,
                new BigDecimal("2.5"));
    }
}
