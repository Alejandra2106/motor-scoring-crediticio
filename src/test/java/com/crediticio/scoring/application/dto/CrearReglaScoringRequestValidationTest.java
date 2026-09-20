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

class CrearReglaScoringRequestValidationTest {

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
        Set<ConstraintViolation<CrearReglaScoringRequest>> violaciones = validator.validate(solicitudValida());
        assertThat(violaciones).isEmpty();
    }

    @Test
    void debeRechazarIdRiesgoNulo() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setIdRiesgo(null);
        assertThat(violacionesEn(request, "idRiesgo")).isNotEmpty();
    }

    @Test
    void debeRechazarOperadorNulo() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setOperador(null);
        assertThat(violacionesEn(request, "operador")).isNotEmpty();
    }

    @Test
    void debeRechazarValorCondicionNulo() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setValorCondicion(null);
        assertThat(violacionesEn(request, "valorCondicion")).isNotEmpty();
    }

    @Test
    void debeRechazarValorCondicionEnBlanco() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setValorCondicion("   ");
        assertThat(violacionesEn(request, "valorCondicion")).isNotEmpty();
    }

    @Test
    void debeRechazarPuntajeNulo() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setPuntaje(null);
        assertThat(violacionesEn(request, "puntaje")).isNotEmpty();
    }

    @Test
    void debeRechazarPuntajeMenorAMenos100() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setPuntaje(-101);
        assertThat(violacionesEn(request, "puntaje")).isNotEmpty();
    }

    @Test
    void debeRechazarPuntajeMayorA100() {
        CrearReglaScoringRequest request = solicitudValida();
        request.setPuntaje(101);
        assertThat(violacionesEn(request, "puntaje")).isNotEmpty();
    }

    @Test
    void debeAceptarPuntajeEnLosLimitesDelRango() {
        CrearReglaScoringRequest request = solicitudValida();

        request.setPuntaje(-100);
        assertThat(violacionesEn(request, "puntaje")).isEmpty();

        request.setPuntaje(100);
        assertThat(violacionesEn(request, "puntaje")).isEmpty();
    }

    private Set<ConstraintViolation<CrearReglaScoringRequest>> violacionesEn(CrearReglaScoringRequest request,
            String propiedad) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(propiedad))
                .collect(Collectors.toSet());
    }

    private CrearReglaScoringRequest solicitudValida() {
        return new CrearReglaScoringRequest(1L, OperadorScoring.MAYOR_O_IGUAL, "3000000", 20);
    }
}
