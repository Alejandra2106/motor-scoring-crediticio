package com.crediticio.scoring.application.dto;

import com.crediticio.scoring.domain.CategoriaRiesgo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU11 CA04: los límites de los rangos solo aceptan enteros JSON; nunca se truncan decimales. */
class PublicarPoliticaClasificacionRequestDeserializacionTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static String rango(String limiteInferior, String limiteSuperior) {
        return "{\"categoria\": \"BAJO\", \"limiteInferior\": " + limiteInferior
                + ", \"limiteSuperior\": " + limiteSuperior + "}";
    }

    @Test
    void debeAceptarLimitesEnterosIncluidosLosExtremos() {
        RangoClasificacionRequest rango = JSON.readValue(rango("-100", "130"), RangoClasificacionRequest.class);

        assertThat(rango.getCategoria()).isEqualTo(CategoriaRiesgo.BAJO);
        assertThat(rango.getLimiteInferior()).isEqualTo(-100);
        assertThat(rango.getLimiteSuperior()).isEqualTo(130);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.7", "10.0", "1e2", "\"10\"", "true"})
    void debeRechazarUnLimiteInferiorQueNoEsUnEnteroJson(String valor) {
        assertThatThrownBy(() -> JSON.readValue(rango(valor, "130"), RangoClasificacionRequest.class))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("número entero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.7", "10.0", "1e2", "\"10\"", "true"})
    void debeRechazarUnLimiteSuperiorQueNoEsUnEnteroJson(String valor) {
        assertThatThrownBy(() -> JSON.readValue(rango("-100", valor), RangoClasificacionRequest.class))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("número entero");
    }

    @Test
    void unLimiteNuloDebeLlegarComoNuloParaQueLoRechaceLaValidacion() {
        RangoClasificacionRequest rango = JSON.readValue(rango("null", "130"), RangoClasificacionRequest.class);

        assertThat(rango.getLimiteInferior()).isNull();
    }

    @Test
    void debeRechazarUnaPoliticaConUnDecimalEnCualquieraDeSusRangos() {
        String politica = "{\"rangos\": [" + rango("-100", "29") + ", " + rango("30", "79.5") + ", "
                + rango("80", "130") + "]}";

        assertThatThrownBy(() -> JSON.readValue(politica, PublicarPoliticaClasificacionRequest.class))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("número entero");
    }
}
