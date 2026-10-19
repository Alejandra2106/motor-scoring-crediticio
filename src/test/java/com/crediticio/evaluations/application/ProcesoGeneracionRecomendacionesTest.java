package com.crediticio.evaluations.application;

import com.crediticio.evaluations.domain.PoliticaReintentos;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import com.crediticio.evaluations.ports.output.ResultadoGeneracion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU14 CA13–CA15: registro de cada resultado de la generación con el token de la concesión. */
class ProcesoGeneracionRecomendacionesTest {

    private static final UUID TOKEN = UUID.randomUUID();
    private static final PoliticaReintentos POLITICA = new PoliticaReintentos(10, Duration.ofSeconds(5),
            Duration.ofSeconds(300), 0.2, Duration.ofSeconds(30), () -> 0.5);

    private final TrabajoRecomendacionPortSimulado trabajo = new TrabajoRecomendacionPortSimulado();

    private ProcesoGeneracionRecomendaciones proceso(ResultadoGeneracion resultado) {
        return new ProcesoGeneracionRecomendaciones((id, modo) -> resultado, trabajo, POLITICA);
    }

    @Test
    void unaGeneracionConfirmadaDebeCompletarLaEvaluacion() {
        assertThat(proceso(new ResultadoGeneracion.Exito("2026.1", 2)).intentar(7, TOKEN, ModoIntento.EN_LINEA, 1))
                .isTrue();
        assertThat(trabajo.llamadas).containsExactly("exito:7");
    }

    /** CA15: si otro trabajador tiene la concesión vigente, el éxito no se registra con el token antiguo. */
    @Test
    void conUnaConcesionYaNoVigenteElExitoSeDescarta() {
        trabajo.registroVigente = false;

        assertThat(proceso(new ResultadoGeneracion.Exito("2026.1", 2)).intentar(7, TOKEN, ModoIntento.REENVIO, 3))
                .isFalse();
    }

    @Test
    void unErrorTransitorioDebeProgramarElSiguienteIntentoConEsperaCreciente() {
        assertThat(proceso(new ResultadoGeneracion.ErrorTransitorio("SIN_CONEXION", "x"))
                .intentar(7, TOKEN, ModoIntento.REENVIO, 3)).isFalse();
        assertThat(trabajo.llamadas).containsExactly("transitorio:7:SIN_CONEXION:20");
    }

    @Test
    void elUltimoIntentoTransitorioDebeMarcarErrorPermanente() {
        proceso(new ResultadoGeneracion.ErrorTransitorio("TIEMPO_AGOTADO", "x")).intentar(7, TOKEN,
                ModoIntento.REENVIO, 10);

        assertThat(trabajo.llamadas).containsExactly("permanente:7:REINTENTOS_AGOTADOS");
    }

    @Test
    void unErrorPermanenteNoDebeReintentarse() {
        proceso(new ResultadoGeneracion.ErrorPermanente("IDENTIDAD_RECHAZADA", "HTTP 401")).intentar(7, TOKEN,
                ModoIntento.EN_LINEA, 1);

        assertThat(trabajo.llamadas).containsExactly("permanente:7:IDENTIDAD_RECHAZADA");
    }

    @Test
    void conLaSagaDeshabilitadaNoDebeRegistrarseNada() {
        assertThat(proceso(new ResultadoGeneracion.NoDisponible()).intentar(7, TOKEN, ModoIntento.EN_LINEA, 1))
                .isFalse();
        assertThat(trabajo.llamadas).isEmpty();
    }
}
