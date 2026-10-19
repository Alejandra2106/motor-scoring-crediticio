package com.crediticio.evaluations.application;

import com.crediticio.evaluations.domain.PoliticaReintentos;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import com.crediticio.evaluations.ports.output.ResultadoGeneracion;
import com.crediticio.evaluations.ports.output.ResultadoReclamo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU14 CA14, CA16: el reenvío reclama de uno en uno y reintenta con el número de intento de la outbox. */
class ReenvioRecomendacionesServiceTest {

    private static final PoliticaReintentos POLITICA = new PoliticaReintentos(10, Duration.ofSeconds(5),
            Duration.ofSeconds(300), 0.2, Duration.ofSeconds(30), () -> 0.5);

    private final TrabajoRecomendacionPortSimulado trabajo = new TrabajoRecomendacionPortSimulado();
    private final List<String> generaciones = new ArrayList<>();

    private ReenvioRecomendacionesService servicio() {
        ProcesoGeneracionRecomendaciones proceso = new ProcesoGeneracionRecomendaciones((id, modo) -> {
            generaciones.add(id + ":" + modo);
            return new ResultadoGeneracion.Exito("2026.1", 1);
        }, trabajo, POLITICA);
        return new ReenvioRecomendacionesService(trabajo, proceso, POLITICA);
    }

    @Test
    void debeProcesarLosTrabajosReclamadosHastaQueNoQuedenPendientes() {
        trabajo.reclamos.add(new ResultadoReclamo.Reclamado(4, UUID.randomUUID(), 2));
        trabajo.reclamos.add(new ResultadoReclamo.Agotado(5));
        trabajo.reclamos.add(new ResultadoReclamo.Reclamado(6, UUID.randomUUID(), 3));

        assertThat(servicio().ejecutarCiclo(20)).isEqualTo(3);
        assertThat(generaciones).containsExactly("4:" + ModoIntento.REENVIO, "6:" + ModoIntento.REENVIO);
        assertThat(trabajo.llamadas).containsExactly("reclamar:10:30", "exito:4", "reclamar:10:30", "reclamar:10:30",
                "exito:6", "reclamar:10:30");
    }

    @Test
    void debeRespetarElMaximoDeTrabajosPorCiclo() {
        for (int i = 0; i < 5; i++) {
            trabajo.reclamos.add(new ResultadoReclamo.Reclamado(i, UUID.randomUUID(), 2));
        }

        assertThat(servicio().ejecutarCiclo(2)).isEqualTo(2);
        assertThat(generaciones).hasSize(2);
    }

    @Test
    void sinTrabajosPendientesNoDebeLlamarAEvaluationQueryApi() {
        assertThat(servicio().ejecutarCiclo(20)).isZero();
        assertThat(generaciones).isEmpty();
    }
}
