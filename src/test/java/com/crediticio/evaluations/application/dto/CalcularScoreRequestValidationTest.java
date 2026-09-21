package com.crediticio.evaluations.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalcularScoreRequestValidationTest {

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
    void unaSolicitudConSoloIdSolicitanteNoDebeGenerarViolaciones() {
        Set<ConstraintViolation<CalcularScoreRequest>> violaciones = validator.validate(
                new CalcularScoreRequest(1L, null));

        assertThat(violaciones).isEmpty();
    }

    @Test
    void unaSolicitudConSoloNumeroDocumentoNoDebeGenerarViolaciones() {
        Set<ConstraintViolation<CalcularScoreRequest>> violaciones = validator.validate(
                new CalcularScoreRequest(null, "1234567890"));

        assertThat(violaciones).isEmpty();
    }

    @Test
    void debeRechazarCuandoSeEnvianAmbosIdentificadores() {
        Set<ConstraintViolation<CalcularScoreRequest>> violaciones = validator.validate(
                new CalcularScoreRequest(1L, "1234567890"));

        assertThat(violaciones).isNotEmpty();
    }

    @Test
    void debeRechazarCuandoNoSeEnviaNingunIdentificador() {
        Set<ConstraintViolation<CalcularScoreRequest>> violaciones = validator.validate(
                new CalcularScoreRequest(null, null));

        assertThat(violaciones).isNotEmpty();
    }

    @Test
    void debeRechazarNumeroDocumentoConFormatoInvalido() {
        Set<ConstraintViolation<CalcularScoreRequest>> violaciones = validator.validate(
                new CalcularScoreRequest(null, "abc"));

        assertThat(violaciones).isNotEmpty();
    }
}
