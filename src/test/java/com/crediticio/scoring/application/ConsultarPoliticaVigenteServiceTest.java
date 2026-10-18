package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.scoring.domain.ConfiguracionClasificacionNoInicializadaException;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import com.crediticio.scoring.domain.PoliticaVigenteNoDisponibleException;
import com.crediticio.scoring.domain.RangoClasificacion;
import com.crediticio.scoring.ports.output.PoliticaClasificacionConsultaPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarPoliticaVigenteServiceTest {

    @Mock
    private PoliticaClasificacionConsultaPort politicaClasificacionConsultaPort;

    private ConsultarPoliticaVigenteService consultarPoliticaVigenteService;

    @BeforeEach
    void inicializarServicio() {
        consultarPoliticaVigenteService = new ConsultarPoliticaVigenteService(
                politicaClasificacionConsultaPort, new PoliticaClasificacionMapper());
    }

    @Test
    void debeRetornarLaPoliticaVigenteConSuVersionFechasYRangos() {
        LocalDateTime instante = LocalDateTime.of(2026, 10, 16, 9, 30, 15, 123_456_000);
        when(politicaClasificacionConsultaPort.buscarVigente()).thenReturn(Optional.of(
                PoliticaClasificacion.reconstruir(7L, 3, instante, instante, null, List.of(
                        RangoClasificacion.reconstruir(22L, CategoriaRiesgo.ALTO, 80, 130),
                        RangoClasificacion.reconstruir(20L, CategoriaRiesgo.BAJO, -100, 29),
                        RangoClasificacion.reconstruir(21L, CategoriaRiesgo.MEDIO, 30, 79)))));

        PoliticaClasificacionResponse response = consultarPoliticaVigenteService.consultarVigente();

        assertThat(response.getIdPolitica()).isEqualTo(7L);
        assertThat(response.getVersion()).isEqualTo(3);
        assertThat(response.getFechaCreacion()).isEqualTo(instante);
        assertThat(response.getFechaPublicacion()).isEqualTo(instante);
        assertThat(response.getRangos()).hasSize(3);
        assertThat(response.getRangos().get(0).getIdRango()).isEqualTo(20L);
        assertThat(response.getRangos().get(0).getCategoria()).isEqualTo(CategoriaRiesgo.BAJO);
        assertThat(response.getRangos().get(0).getLimiteInferior()).isEqualTo(-100);
        assertThat(response.getRangos().get(1).getCategoria()).isEqualTo(CategoriaRiesgo.MEDIO);
        assertThat(response.getRangos().get(2).getCategoria()).isEqualTo(CategoriaRiesgo.ALTO);
        assertThat(response.getRangos().get(2).getLimiteSuperior()).isEqualTo(130);
    }

    @Test
    void sinPoliticaVigenteDebeInformarExplicitamenteSuAusencia() {
        when(politicaClasificacionConsultaPort.buscarVigente()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultarPoliticaVigenteService.consultarVigente())
                .isInstanceOf(PoliticaVigenteNoDisponibleException.class)
                .hasMessage("No existe una política de clasificación vigente");
    }

    @Test
    void debePropagarLaExcepcionDeConfiguracionNoInicializada() {
        when(politicaClasificacionConsultaPort.buscarVigente())
                .thenThrow(new ConfiguracionClasificacionNoInicializadaException());

        assertThatThrownBy(() -> consultarPoliticaVigenteService.consultarVigente())
                .isInstanceOf(ConfiguracionClasificacionNoInicializadaException.class);
    }
}
