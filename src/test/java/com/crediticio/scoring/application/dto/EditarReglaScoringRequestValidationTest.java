package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.OperadorScoring;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EditarReglaScoringRequestValidationTest {

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
        Set<ConstraintViolation<EditarReglaScoringRequest>> violaciones = validator.validate(solicitudValida());
        assertThat(violaciones).isEmpty();
    }

    @Test
    void debeRechazarOperadorNulo() {
        EditarReglaScoringRequest request = solicitudValida();
        request.setOperador(null);
        assertThat(violacionesEn(request, "operador")).isNotEmpty();
    }

    @Test
    void debeRechazarValorCondicionNulo() {
        EditarReglaScoringRequest request = solicitudValida();
        request.setValorCondicion(null);
        assertThat(violacionesEn(request, "valorCondicion")).isNotEmpty();
    }

    @Test
    void debeRechazarValorCondicionEnBlanco() {
        EditarReglaScoringRequest request = solicitudValida();
        request.setValorCondicion("   ");
        assertThat(violacionesEn(request, "valorCondicion")).isNotEmpty();
    }

    @Test
    void debeRechazarPuntajeNulo() {
        EditarReglaScoringRequest request = solicitudValida();
        request.setPuntaje(null);
        assertThat(violacionesEn(request, "puntaje")).isNotEmpty();
    }

    @Test
    void debeRechazarPuntajeMenorAMenos100() {
        EditarReglaScoringRequest request = solicitudValida();
        request.setPuntaje(-101);
        assertThat(violacionesEn(request, "puntaje")).isNotEmpty();
    }

    @Test
    void debeRechazarPuntajeMayorA100() {
        EditarReglaScoringRequest request = solicitudValida();
        request.setPuntaje(101);
        assertThat(violacionesEn(request, "puntaje")).isNotEmpty();
    }

    @Test
    void debeAceptarPuntajeEnLosLimitesDelRango() {
        EditarReglaScoringRequest request = solicitudValida();

        request.setPuntaje(-100);
        assertThat(violacionesEn(request, "puntaje")).isEmpty();

        request.setPuntaje(100);
        assertThat(violacionesEn(request, "puntaje")).isEmpty();
    }

    private Set<ConstraintViolation<EditarReglaScoringRequest>> violacionesEn(EditarReglaScoringRequest request,
            String propiedad) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(propiedad))
                .collect(Collectors.toSet());
    }

    private EditarReglaScoringRequest solicitudValida() {
        return new EditarReglaScoringRequest(OperadorScoring.MAYOR_O_IGUAL, "5000000", 25);
    }
}
