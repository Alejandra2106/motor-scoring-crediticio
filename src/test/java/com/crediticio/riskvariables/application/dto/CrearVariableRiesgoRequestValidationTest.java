package com.crediticio.riskvariables.application.dto;

import com.crediticio.riskvariables.domain.NombreVariableRiesgo;
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

class CrearVariableRiesgoRequestValidationTest {

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
        Set<ConstraintViolation<CrearVariableRiesgoRequest>> violaciones = validator.validate(solicitudValida());
        assertThat(violaciones).isEmpty();
    }

    @Test
    void debeRechazarVariableNula() {
        CrearVariableRiesgoRequest request = solicitudValida();
        request.setVariable(null);
        assertThat(violacionesEn(request, "variable")).isNotEmpty();
    }

    @Test
    void debeRechazarDescripcionEnBlanco() {
        CrearVariableRiesgoRequest request = solicitudValida();
        request.setDescripcion("   ");
        assertThat(violacionesEn(request, "descripcion")).isNotEmpty();
    }

    @Test
    void debeRechazarDescripcionNula() {
        CrearVariableRiesgoRequest request = solicitudValida();
        request.setDescripcion(null);
        assertThat(violacionesEn(request, "descripcion")).isNotEmpty();
    }

    @Test
    void debeRechazarDescripcionMuyCorta() {
        CrearVariableRiesgoRequest request = solicitudValida();
        request.setDescripcion("corta");
        assertThat(violacionesEn(request, "descripcion")).isNotEmpty();
    }

    @Test
    void debeRechazarDescripcionMayorA255Caracteres() {
        CrearVariableRiesgoRequest request = solicitudValida();
        request.setDescripcion("A".repeat(256));
        assertThat(violacionesEn(request, "descripcion")).isNotEmpty();
    }

    @Test
    void debeAceptarDescripcionEnLosLimitesDeLongitud() {
        CrearVariableRiesgoRequest request = solicitudValida();
        request.setDescripcion("1234567890");
        assertThat(violacionesEn(request, "descripcion")).isEmpty();

        request.setDescripcion("A".repeat(255));
        assertThat(violacionesEn(request, "descripcion")).isEmpty();
    }

    private Set<ConstraintViolation<CrearVariableRiesgoRequest>> violacionesEn(CrearVariableRiesgoRequest request,
            String propiedad) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(propiedad))
                .collect(Collectors.toSet());
    }

    private CrearVariableRiesgoRequest solicitudValida() {
        return new CrearVariableRiesgoRequest(
                NombreVariableRiesgo.INGRESOS_MENSUALES,
                "Ingresos mensuales netos declarados por el solicitante");
    }
}
