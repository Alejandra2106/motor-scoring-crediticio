package com.crediticio.evaluations.domain;

import java.util.Objects;

/**
 * Detalle de una regla considerada durante una evaluación. Conserva, además del resultado
 * ({@code condicionCumplida} y {@code puntajeObtenido}), un snapshot de la condición de la
 * regla tal como fue aplicada en ese momento ({@code operadorAplicado}, {@code
 * valorCondicionAplicado} y {@code puntajeReglaAplicado}), porque {@code regla_scoring} puede
 * editarse en el mismo registro (HU06) y no debe depender de un JOIN futuro para reconstruir
 * el histórico (RF11/RNF06).
 */
public class DetalleEvaluacion {

    private final Long idDetalleEvaluacion;
    private final Long idRegla;
    private final String operadorAplicado;
    private final String valorCondicionAplicado;
    private final Integer puntajeReglaAplicado;
    private final boolean condicionCumplida;
    private final Integer puntajeObtenido;

    private DetalleEvaluacion(Long idDetalleEvaluacion, Long idRegla, String operadorAplicado,
            String valorCondicionAplicado, Integer puntajeReglaAplicado, boolean condicionCumplida,
            Integer puntajeObtenido) {
        this.idDetalleEvaluacion = idDetalleEvaluacion;
        this.idRegla = Objects.requireNonNull(idRegla, "idRegla es obligatorio");
        this.operadorAplicado = validarNoVacio(operadorAplicado, "operadorAplicado");
        this.valorCondicionAplicado = validarNoVacio(valorCondicionAplicado, "valorCondicionAplicado");
        this.puntajeReglaAplicado = Objects.requireNonNull(puntajeReglaAplicado, "puntajeReglaAplicado es obligatorio");
        this.condicionCumplida = condicionCumplida;
        this.puntajeObtenido = validarPuntajeObtenido(puntajeObtenido, condicionCumplida, puntajeReglaAplicado);
    }

    public static DetalleEvaluacion nuevo(Long idRegla, String operadorAplicado, String valorCondicionAplicado,
            Integer puntajeReglaAplicado, boolean condicionCumplida, Integer puntajeObtenido) {
        return new DetalleEvaluacion(null, idRegla, operadorAplicado, valorCondicionAplicado, puntajeReglaAplicado,
                condicionCumplida, puntajeObtenido);
    }

    public static DetalleEvaluacion reconstruir(Long idDetalleEvaluacion, Long idRegla, String operadorAplicado,
            String valorCondicionAplicado, Integer puntajeReglaAplicado, boolean condicionCumplida,
            Integer puntajeObtenido) {
        return new DetalleEvaluacion(idDetalleEvaluacion, idRegla, operadorAplicado, valorCondicionAplicado,
                puntajeReglaAplicado, condicionCumplida, puntajeObtenido);
    }

    private static String validarNoVacio(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " es obligatorio");
        }
        return valor;
    }

    private static Integer validarPuntajeObtenido(Integer puntajeObtenido, boolean condicionCumplida,
            Integer puntajeReglaAplicado) {
        Objects.requireNonNull(puntajeObtenido, "puntajeObtenido es obligatorio");
        Integer esperado = condicionCumplida ? puntajeReglaAplicado : 0;
        if (!puntajeObtenido.equals(esperado)) {
            throw new IllegalArgumentException(
                    "puntajeObtenido debe ser " + esperado + " cuando condicionCumplida es " + condicionCumplida);
        }
        return puntajeObtenido;
    }

    public Long getIdDetalleEvaluacion() {
        return idDetalleEvaluacion;
    }

    public Long getIdRegla() {
        return idRegla;
    }

    public String getOperadorAplicado() {
        return operadorAplicado;
    }

    public String getValorCondicionAplicado() {
        return valorCondicionAplicado;
    }

    public Integer getPuntajeReglaAplicado() {
        return puntajeReglaAplicado;
    }

    public boolean isCondicionCumplida() {
        return condicionCumplida;
    }

    public Integer getPuntajeObtenido() {
        return puntajeObtenido;
    }
}
