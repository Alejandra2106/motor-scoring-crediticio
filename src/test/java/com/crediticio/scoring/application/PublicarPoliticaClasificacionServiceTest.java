package com.crediticio.scoring.application;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;
import com.crediticio.scoring.application.dto.RangoClasificacionRequest;
import com.crediticio.scoring.domain.CategoriaRiesgo;
import com.crediticio.scoring.domain.ConfiguracionClasificacionNoInicializadaException;
import com.crediticio.scoring.domain.PoliticaClasificacion;
import com.crediticio.scoring.domain.PoliticaClasificacionInvalidaException;
import com.crediticio.scoring.domain.PublicacionPoliticaConcurrenteException;
import com.crediticio.scoring.domain.PublicacionSinAutorException;
import com.crediticio.scoring.domain.RangoClasificacion;
import com.crediticio.scoring.ports.output.PoliticaClasificacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicarPoliticaClasificacionServiceTest {

    private static final UUID AUTOR = UUID.fromString("6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a4b");

    @Mock
    private PoliticaClasificacionRepositoryPort politicaClasificacionRepositoryPort;

    private PublicarPoliticaClasificacionService publicarPoliticaClasificacionService;

    @BeforeEach
    void inicializarServicio() {
        publicarPoliticaClasificacionService = new PublicarPoliticaClasificacionService(
                politicaClasificacionRepositoryPort, () -> Optional.of(AUTOR), new PoliticaClasificacionMapper());
    }

    @Test
    void debeRegistrarComoAutorAlUsuarioAutenticado() {
        when(politicaClasificacionRepositoryPort.publicar(any(PoliticaClasificacion.class)))
                .thenAnswer(invocation -> comoPublicada(invocation.getArgument(0)));

        publicarPoliticaClasificacionService.publicar(solicitudValida());

        ArgumentCaptor<PoliticaClasificacion> captor = ArgumentCaptor.forClass(PoliticaClasificacion.class);
        verify(politicaClasificacionRepositoryPort).publicar(captor.capture());
        assertThat(captor.getValue().getPublicadoPor()).isEqualTo(AUTOR);
    }

    @Test
    void sinUsuarioAutenticadoDebeRechazarseSinPersistirNiInventarUnAutor() {
        PublicarPoliticaClasificacionService sinAutor = new PublicarPoliticaClasificacionService(
                politicaClasificacionRepositoryPort, Optional::empty, new PoliticaClasificacionMapper());

        assertThatThrownBy(() -> sinAutor.publicar(solicitudValida()))
                .isInstanceOf(PublicacionSinAutorException.class);

        verify(politicaClasificacionRepositoryPort, never()).publicar(any(PoliticaClasificacion.class));
    }

    @Test
    void debePublicarLaPoliticaUnaSolaVezYRetornarLaVersionYLosRangosPersistidos() {
        when(politicaClasificacionRepositoryPort.publicar(any(PoliticaClasificacion.class)))
                .thenAnswer(invocation -> comoPublicada(invocation.getArgument(0)));

        PoliticaClasificacionResponse response = publicarPoliticaClasificacionService.publicar(solicitudValida());

        verify(politicaClasificacionRepositoryPort, times(1)).publicar(any(PoliticaClasificacion.class));
        assertThat(response.getIdPolitica()).isEqualTo(4L);
        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getRangos()).hasSize(3);
        assertThat(response.getRangos().get(0).getIdRango()).isEqualTo(100L);
        assertThat(response.getRangos().get(0).getCategoria()).isEqualTo(CategoriaRiesgo.BAJO);
        assertThat(response.getRangos().get(0).getLimiteInferior()).isEqualTo(-100);
        assertThat(response.getRangos().get(2).getLimiteSuperior()).isEqualTo(130);
    }

    @Test
    void debeUsarUnUnicoInstanteCapturadoAlInicioParaAmbasFechas() {
        when(politicaClasificacionRepositoryPort.publicar(any(PoliticaClasificacion.class)))
                .thenAnswer(invocation -> comoPublicada(invocation.getArgument(0)));
        LocalDateTime antes = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        PoliticaClasificacionResponse response = publicarPoliticaClasificacionService.publicar(solicitudValida());

        LocalDateTime despues = LocalDateTime.now();
        ArgumentCaptor<PoliticaClasificacion> captor = ArgumentCaptor.forClass(PoliticaClasificacion.class);
        verify(politicaClasificacionRepositoryPort).publicar(captor.capture());
        PoliticaClasificacion enviada = captor.getValue();
        assertThat(enviada.getFechaCreacion()).isEqualTo(enviada.getFechaPublicacion());
        assertThat(enviada.getFechaCreacion()).isBetween(antes, despues);
        assertThat(enviada.getFechaCreacion().getNano() % 1_000).isZero();
        assertThat(response.getFechaCreacion()).isEqualTo(response.getFechaPublicacion());
    }

    @Test
    void unaPoliticaInvalidaNoDebeLlegarAlPuertoDePersistencia() {
        PublicarPoliticaClasificacionRequest request = new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 29),
                new RangoClasificacionRequest(CategoriaRiesgo.MEDIO, 31, 79),
                new RangoClasificacionRequest(CategoriaRiesgo.ALTO, 80, 130)));

        assertThatThrownBy(() -> publicarPoliticaClasificacionService.publicar(request))
                .isInstanceOf(PoliticaClasificacionInvalidaException.class);

        verify(politicaClasificacionRepositoryPort, never()).publicar(any(PoliticaClasificacion.class));
    }

    @Test
    void unaSolicitudSinRangosDebeRechazarseSinPersistir() {
        assertThatThrownBy(() -> publicarPoliticaClasificacionService.publicar(
                new PublicarPoliticaClasificacionRequest(null)))
                .isInstanceOf(PoliticaClasificacionInvalidaException.class);

        verify(politicaClasificacionRepositoryPort, never()).publicar(any(PoliticaClasificacion.class));
    }

    @Test
    void debeRechazarUnaSolicitudNula() {
        assertThatThrownBy(() -> publicarPoliticaClasificacionService.publicar(null))
                .isInstanceOf(NullPointerException.class);

        verify(politicaClasificacionRepositoryPort, never()).publicar(any(PoliticaClasificacion.class));
    }

    @Test
    void debePropagarLaExcepcionDePublicacionConcurrente() {
        when(politicaClasificacionRepositoryPort.publicar(any(PoliticaClasificacion.class)))
                .thenThrow(new PublicacionPoliticaConcurrenteException());

        assertThatThrownBy(() -> publicarPoliticaClasificacionService.publicar(solicitudValida()))
                .isInstanceOf(PublicacionPoliticaConcurrenteException.class);
    }

    @Test
    void debePropagarLaExcepcionDeConfiguracionNoInicializada() {
        when(politicaClasificacionRepositoryPort.publicar(any(PoliticaClasificacion.class)))
                .thenThrow(new ConfiguracionClasificacionNoInicializadaException());

        assertThatThrownBy(() -> publicarPoliticaClasificacionService.publicar(solicitudValida()))
                .isInstanceOf(ConfiguracionClasificacionNoInicializadaException.class);
    }

    private PoliticaClasificacion comoPublicada(PoliticaClasificacion enviada) {
        List<RangoClasificacion> rangos = List.of(
                RangoClasificacion.reconstruir(100L, enviada.getRangos().get(0).getCategoria(),
                        enviada.getRangos().get(0).getLimiteInferior(), enviada.getRangos().get(0).getLimiteSuperior()),
                RangoClasificacion.reconstruir(101L, enviada.getRangos().get(1).getCategoria(),
                        enviada.getRangos().get(1).getLimiteInferior(), enviada.getRangos().get(1).getLimiteSuperior()),
                RangoClasificacion.reconstruir(102L, enviada.getRangos().get(2).getCategoria(),
                        enviada.getRangos().get(2).getLimiteInferior(), enviada.getRangos().get(2).getLimiteSuperior()));
        return PoliticaClasificacion.reconstruir(4L, 2, enviada.getFechaCreacion(), enviada.getFechaPublicacion(),
                enviada.getPublicadoPor(), rangos);
    }

    private PublicarPoliticaClasificacionRequest solicitudValida() {
        return new PublicarPoliticaClasificacionRequest(List.of(
                new RangoClasificacionRequest(CategoriaRiesgo.BAJO, -100, 29),
                new RangoClasificacionRequest(CategoriaRiesgo.MEDIO, 30, 79),
                new RangoClasificacionRequest(CategoriaRiesgo.ALTO, 80, 130)));
    }
}
