package com.crediticio.scoring.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaClasificacionTest {

    private static final LocalDateTime INSTANTE = LocalDateTime.of(2026, 10, 16, 9, 30, 15, 123_456_000);
    private static final UUID AUTOR = UUID.fromString("6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a4b");

    @Test
    void unaPoliticaNuevaDebeRegistrarSuAutor() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, definicionesValidas());

        assertThat(politica.getPublicadoPor()).isEqualTo(AUTOR);
    }

    @Test
    void unaPoliticaNuevaSinAutorDebeRechazarse() {
        assertThatThrownBy(() -> PoliticaClasificacion.nueva(INSTANTE, null, definicionesValidas()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("publicadoPor");
    }

    @Test
    void reconstruirDebeConservarElAutor() {
        PoliticaClasificacion politica = PoliticaClasificacion.reconstruir(5L, 3, INSTANTE, INSTANTE, AUTOR, List.of(
                RangoClasificacion.reconstruir(11L, CategoriaRiesgo.BAJO, -100, 29),
                RangoClasificacion.reconstruir(12L, CategoriaRiesgo.MEDIO, 30, 79),
                RangoClasificacion.reconstruir(13L, CategoriaRiesgo.ALTO, 80, 130)));

        assertThat(politica.getPublicadoPor()).isEqualTo(AUTOR);
    }

    @Test
    void unaPoliticaHistoricaSinAutorDebePoderReconstruirse() {
        PoliticaClasificacion historica = PoliticaClasificacion.reconstruir(1L, 1, INSTANTE, INSTANTE, null, List.of(
                RangoClasificacion.reconstruir(1L, CategoriaRiesgo.BAJO, -100, 29),
                RangoClasificacion.reconstruir(2L, CategoriaRiesgo.MEDIO, 30, 79),
                RangoClasificacion.reconstruir(3L, CategoriaRiesgo.ALTO, 80, 130)));

        assertThat(historica.getPublicadoPor()).isNull();
        assertThat(historica.clasificar(50)).contains(CategoriaRiesgo.MEDIO);
    }

    @Test
    void debeCrearUnaPoliticaValidaSinIdNiVersionYConAmbasFechasIgualesAlInstante() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130)));

        assertThat(politica.getIdPolitica()).isNull();
        assertThat(politica.getVersion()).isNull();
        assertThat(politica.getFechaCreacion()).isEqualTo(INSTANTE);
        assertThat(politica.getFechaPublicacion()).isEqualTo(INSTANTE);
        assertThat(politica.getRangos()).hasSize(3);
    }

    @Test
    void debeOrdenarLosRangosPorLimiteInferior() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, List.of(
                rango(CategoriaRiesgo.ALTO, 80, 130),
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79)));

        assertThat(politica.getRangos())
                .extracting(RangoClasificacion::getLimiteInferior)
                .containsExactly(-100, 30, 80);
    }

    @Test
    void noDebeImponerUnOrdenEntreCategorias() {
        PoliticaClasificacion invertida = PoliticaClasificacion.nueva(INSTANTE, AUTOR, List.of(
                rango(CategoriaRiesgo.BAJO, 80, 130),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, -100, 29)));

        assertThat(invertida.getRangos())
                .extracting(RangoClasificacion::getCategoria)
                .containsExactly(CategoriaRiesgo.ALTO, CategoriaRiesgo.MEDIO, CategoriaRiesgo.BAJO);
    }

    @Test
    void debeAceptarUnaPoliticaValidaSinExigirUnOrdenFijoEntreCategorias() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, List.of(
                rango(CategoriaRiesgo.ALTO, -100, 29),
                rango(CategoriaRiesgo.BAJO, 30, 79),
                rango(CategoriaRiesgo.MEDIO, 80, 130)));

        assertThat(politica.getRangos())
                .extracting(RangoClasificacion::getCategoria)
                .containsExactly(CategoriaRiesgo.ALTO, CategoriaRiesgo.BAJO, CategoriaRiesgo.MEDIO);
        assertThat(politica.getRangos())
                .extracting(RangoClasificacion::getLimiteInferior)
                .containsExactly(-100, 30, 80);
        assertThat(politica.getRangos())
                .extracting(RangoClasificacion::getLimiteSuperior)
                .containsExactly(29, 79, 130);
    }

    @Test
    void debeAceptarRangosDeUnSoloValorEnLosLimitesInclusivos() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, List.of(
                rango(CategoriaRiesgo.MEDIO, -100, -100),
                rango(CategoriaRiesgo.BAJO, -99, 129),
                rango(CategoriaRiesgo.ALTO, 130, 130)));

        assertThat(politica.getRangos())
                .extracting(RangoClasificacion::getLimiteSuperior)
                .containsExactly(-100, 129, 130);
    }

    @Test
    void losRangosDeLaPoliticaNoDebenPoderModificarse() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, definicionesValidas());

        assertThatThrownBy(() -> politica.getRangos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void modificarLaListaDeEntradaNoDebeAlterarLaPoliticaCreada() {
        List<DefinicionRango> definiciones = new ArrayList<>(definicionesValidas());
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, definiciones);

        definiciones.clear();

        assertThat(politica.getRangos()).hasSize(3);
    }

    @Test
    void debeRechazarInstanteNulo() {
        assertThatThrownBy(() -> PoliticaClasificacion.nueva(null, AUTOR, definicionesValidas()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void debeRechazarDefinicionesNulas() {
        assertThat(errores(null))
                .containsExactly("La política debe contener exactamente 3 rangos");
    }

    @Test
    void debeRechazarUnaPoliticaSinRangos() {
        assertThat(errores(List.of()))
                .containsExactly("La política debe contener exactamente 3 rangos");
    }

    @Test
    void debeRechazarUnaPoliticaConSoloDosRangos() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 130))))
                .contains("La política debe contener exactamente 3 rangos y contiene 2",
                        "Falta el rango de la categoría ALTO");
    }

    @Test
    void debeRechazarUnaPoliticaConCuatroRangos() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 120),
                rango(CategoriaRiesgo.ALTO, 121, 130))))
                .contains("La política debe contener exactamente 3 rangos y contiene 4",
                        "La categoría ALTO está duplicada");
    }

    @Test
    void debeRechazarCategoriaDuplicadaYReportarLaCategoriaFaltante() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.BAJO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .contains("La categoría BAJO está duplicada", "Falta el rango de la categoría MEDIO");
    }

    @Test
    void debeRechazarUnRangoNulo() {
        assertThat(errores(Arrays.asList(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                null,
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .contains("rangos[1]: el rango es obligatorio", "Falta el rango de la categoría MEDIO");
    }

    @Test
    void debeRechazarCategoriaNula() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(null, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .contains("rangos[1]: categoria es obligatoria", "Falta el rango de la categoría MEDIO");
    }

    @Test
    void debeRechazarLimitesNulos() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, null, 29),
                rango(CategoriaRiesgo.MEDIO, 30, null),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .contains("rangos[0]: limiteInferior es obligatorio", "rangos[1]: limiteSuperior es obligatorio");
    }

    @Test
    void debeRechazarUnLimiteInferiorMenorQueMenosCien() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -101, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .contains("rangos[0]: limiteInferior debe estar entre -100 y 130");
    }

    @Test
    void debeRechazarUnLimiteSuperiorMayorQueCientoTreinta() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 131))))
                .contains("rangos[2]: limiteSuperior debe estar entre -100 y 130");
    }

    @Test
    void debeRechazarUnLimiteInferiorMayorQueElSuperior() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 79, 30),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .contains("rangos[1]: limiteInferior no puede ser mayor que limiteSuperior");
    }

    @Test
    void debeRechazarUnHuecoEntreRangosConsecutivos() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 31, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .containsExactly("Los valores entre 30 y 30 no están cubiertos por ningún rango");
    }

    @Test
    void debeRechazarRangosSuperpuestos() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 30),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .containsExactly("El rango [30, 79] se superpone con otro rango entre 30 y 30");
    }

    @Test
    void debeRechazarUnaCoberturaQueNoEmpiezaEnMenosCien() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -90, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130))))
                .containsExactly("Los valores entre -100 y -91 no están cubiertos por ningún rango");
    }

    @Test
    void debeRechazarUnaCoberturaQueNoLlegaACientoTreinta() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 120))))
                .containsExactly("Los valores entre 121 y 130 no están cubiertos por ningún rango");
    }

    @Test
    void debeReunirTodosLosErroresEnLugarDeDetenerseEnElPrimero() {
        assertThat(errores(List.of(
                rango(CategoriaRiesgo.BAJO, -101, 29),
                rango(CategoriaRiesgo.BAJO, 79, 30),
                rango(null, 80, 131))))
                .containsExactlyInAnyOrder(
                        "rangos[0]: limiteInferior debe estar entre -100 y 130",
                        "La categoría BAJO está duplicada",
                        "rangos[1]: limiteInferior no puede ser mayor que limiteSuperior",
                        "rangos[2]: categoria es obligatoria",
                        "rangos[2]: limiteSuperior debe estar entre -100 y 130",
                        "Falta el rango de la categoría MEDIO",
                        "Falta el rango de la categoría ALTO");
    }

    @Test
    void reconstruirDebeConservarIdVersionFechasYRangos() {
        PoliticaClasificacion politica = PoliticaClasificacion.reconstruir(5L, 3, INSTANTE, INSTANTE, AUTOR, List.of(
                RangoClasificacion.reconstruir(11L, CategoriaRiesgo.BAJO, -100, 29),
                RangoClasificacion.reconstruir(12L, CategoriaRiesgo.MEDIO, 30, 79),
                RangoClasificacion.reconstruir(13L, CategoriaRiesgo.ALTO, 80, 130)));

        assertThat(politica.getIdPolitica()).isEqualTo(5L);
        assertThat(politica.getVersion()).isEqualTo(3);
        assertThat(politica.getFechaCreacion()).isEqualTo(INSTANTE);
        assertThat(politica.getRangos()).extracting(RangoClasificacion::getIdRango).containsExactly(11L, 12L, 13L);
    }

    @Test
    void reconstruirDebeRechazarUnaVersionNoPositiva() {
        assertThatThrownBy(() -> PoliticaClasificacion.reconstruir(5L, 0, INSTANTE, INSTANTE, AUTOR, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debeRechazarFechasDeCreacionYPublicacionDistintas() {
        assertThatThrownBy(() -> PoliticaClasificacion.reconstruir(5L, 1, INSTANTE, INSTANTE.plusSeconds(1),
                AUTOR, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "-100, BAJO",
            "-1, BAJO",
            "0, BAJO",
            "29, BAJO",
            "30, MEDIO",
            "79, MEDIO",
            "80, ALTO",
            "130, ALTO"})
    void clasificarDebeAsignarLaCategoriaDelRangoQueContieneElPuntajeConLimitesInclusivos(int puntaje,
            CategoriaRiesgo esperada) {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, definicionesValidas());

        assertThat(politica.clasificar(puntaje)).contains(esperada);
    }

    @ParameterizedTest
    @ValueSource(ints = {-101, 131, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void clasificarDebeRetornarVacioCuandoNingunRangoContieneElPuntaje(int puntaje) {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, definicionesValidas());

        assertThat(politica.clasificar(puntaje)).isEmpty();
    }

    @Test
    void clasificarDebeUsarLosRangosDeLaPoliticaSinImponerUnOrdenEntreCategorias() {
        PoliticaClasificacion politica = PoliticaClasificacion.nueva(INSTANTE, AUTOR, List.of(
                rango(CategoriaRiesgo.ALTO, -100, 29),
                rango(CategoriaRiesgo.BAJO, 30, 79),
                rango(CategoriaRiesgo.MEDIO, 80, 130)));

        assertThat(politica.clasificar(-100)).contains(CategoriaRiesgo.ALTO);
        assertThat(politica.clasificar(79)).contains(CategoriaRiesgo.BAJO);
        assertThat(politica.clasificar(130)).contains(CategoriaRiesgo.MEDIO);
    }

    @Test
    void clasificarDebeFuncionarSobreUnaPoliticaReconstruidaDesdePersistencia() {
        PoliticaClasificacion politica = PoliticaClasificacion.reconstruir(5L, 3, INSTANTE, INSTANTE, AUTOR, List.of(
                RangoClasificacion.reconstruir(13L, CategoriaRiesgo.ALTO, 80, 130),
                RangoClasificacion.reconstruir(11L, CategoriaRiesgo.BAJO, -100, 29),
                RangoClasificacion.reconstruir(12L, CategoriaRiesgo.MEDIO, 30, 79)));

        assertThat(politica.clasificar(29)).contains(CategoriaRiesgo.BAJO);
        assertThat(politica.clasificar(30)).contains(CategoriaRiesgo.MEDIO);
        assertThat(politica.clasificar(80)).contains(CategoriaRiesgo.ALTO);
    }

    private List<String> errores(List<DefinicionRango> definiciones) {
        PoliticaClasificacionInvalidaException ex = assertThrows(PoliticaClasificacionInvalidaException.class,
                () -> PoliticaClasificacion.nueva(INSTANTE, AUTOR, definiciones));
        return ex.getErrores();
    }

    private static List<DefinicionRango> definicionesValidas() {
        return List.of(
                rango(CategoriaRiesgo.BAJO, -100, 29),
                rango(CategoriaRiesgo.MEDIO, 30, 79),
                rango(CategoriaRiesgo.ALTO, 80, 130));
    }

    private static DefinicionRango rango(CategoriaRiesgo categoria, Integer limiteInferior, Integer limiteSuperior) {
        return new DefinicionRango(categoria, limiteInferior, limiteSuperior);
    }
}
