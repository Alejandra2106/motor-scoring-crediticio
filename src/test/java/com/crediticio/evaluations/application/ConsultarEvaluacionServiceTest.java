package com.crediticio.evaluations.application;

import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.domain.ClasificacionEvaluacion;
import com.crediticio.evaluations.domain.DetalleEvaluacion;
import com.crediticio.evaluations.domain.Evaluacion;
import com.crediticio.evaluations.domain.EvaluacionNoEncontradaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarEvaluacionServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 2, 11, 30);
    private static final List<DetalleEvaluacion> DETALLES = List.of(
            DetalleEvaluacion.reconstruir(1L, 10L, ">=", "3000000", 20, true, 20),
            DetalleEvaluacion.reconstruir(2L, 11L, "=", "MALO", 30, false, 0));

    @Test
    void debeRetornarPuntajeClasificacionYVersionDePoliticaRegistrados() {
        Evaluacion evaluacion = Evaluacion.reconstruir(5L, 1L, FECHA, 20,
                new ClasificacionEvaluacion("MEDIO", 3L, 2), DETALLES);
        ConsultarEvaluacionService service = new ConsultarEvaluacionService(
                id -> id.equals(5L) ? Optional.of(evaluacion) : Optional.empty(), new EvaluacionMapper());

        EvaluacionResponse response = service.consultarPorId(5L);

        assertThat(response.getIdEvaluacion()).isEqualTo(5L);
        assertThat(response.getIdSolicitante()).isEqualTo(1L);
        assertThat(response.getFechaEvaluacion()).isEqualTo(FECHA);
        assertThat(response.getScoreTotal()).isEqualTo(20);
        assertThat(response.getCategoria()).isEqualTo("MEDIO");
        assertThat(response.getIdPolitica()).isEqualTo(3L);
        assertThat(response.getVersionPolitica()).isEqualTo(2);
        assertThat(response.getDetalles()).hasSize(2);
    }

    @Test
    void unaEvaluacionAnteriorAHu12DebeRetornarseSinClasificacionNiPolitica() {
        Evaluacion historica = Evaluacion.reconstruir(6L, 1L, FECHA, null, null, DETALLES);
        ConsultarEvaluacionService service = new ConsultarEvaluacionService(
                id -> Optional.of(historica), new EvaluacionMapper());

        EvaluacionResponse response = service.consultarPorId(6L);

        assertThat(response.getScoreTotal()).isEqualTo(20);
        assertThat(response.getCategoria()).isNull();
        assertThat(response.getIdPolitica()).isNull();
        assertThat(response.getVersionPolitica()).isNull();
    }

    @Test
    void unaEvaluacionInexistenteDebeLanzarEvaluacionNoEncontrada() {
        ConsultarEvaluacionService service = new ConsultarEvaluacionService(
                id -> Optional.empty(), new EvaluacionMapper());

        assertThatThrownBy(() -> service.consultarPorId(99L)).isInstanceOf(EvaluacionNoEncontradaException.class);
    }
}
