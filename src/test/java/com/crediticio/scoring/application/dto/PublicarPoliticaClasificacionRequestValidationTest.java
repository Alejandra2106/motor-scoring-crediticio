package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.CategoriaRiesgo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PublicarPoliticaClasificacionRequestValidationTest {

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
        assertThat(validator.validate(solicitudValida())).isEmpty();
    }

    @Test
    void debeRechazarRangosNulos() {
        assertThat(rutasConViolacion(new PublicarPoliticaClasificacionRequest(null))).contains("rangos");
    }

    @Test
    void debeRechazarCategoriaNulaEnUnRango() {
        PublicarPoliticaClasificacionRequest request = solicitudValida();
        request.getRangos().get(1).setCategoria(null);

        assertThat(rutasConViolacion(request)).contains("rangos[1].categoria");
    }

    @Test
    void debeRechazarLimitesNulosEnUnRango() {
        PublicarPoliticaClasificacionRequest request = solicitudValida();
        request.getRangos().get(0).setLimiteInferior(null);
        request.getRangos().get(2).setLimiteSuperior(null);

        assertThat(rutasConViolacion(request)).contains("rangos[0].limiteInferior", "rangos[2].limiteSuperior");
    }

    private Set<String> rutasConViolacion(PublicarPoliticaClasificacionRequest request) {
        Set<ConstraintViolation<PublicarPoliticaClasificacionRequest>> violaciones = validator.validate(request);
        return violaciones.stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private PublicarPoliticaClasificacionRequest solicitudValida() {
        return new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 29),
                new RangoClasificacionRequest(CategoriaRiesgo.MEDIO, 30, 79),
                new RangoClasificacionRequest(CategoriaRiesgo.ALTO, 80, 130)));
    }
}
