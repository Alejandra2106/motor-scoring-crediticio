package com.crediticio.evaluations.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

/**
 * Política de reintentos de la saga de recomendaciones (HU14 CA14, CA16; valores aprobados el 2026-10-18):
 * máximo de intentos por ciclo (el primero es el intento en línea), espera creciente con un tope y variación
 * aleatoria, y duración de la concesión de cada intento.
 */
public final class PoliticaReintentos {

    private final int maxIntentos;
    private final Duration esperaInicial;
    private final Duration esperaMaxima;
    private final double variacion;
    private final Duration concesion;
    private final DoubleSupplier aleatorio;

    /**
     * @param variacion fracción de variación aleatoria simétrica (0.2 = ±20 %)
     * @param aleatorio fuente de valores uniformes en [0, 1)
     */
    public PoliticaReintentos(int maxIntentos, Duration esperaInicial, Duration esperaMaxima, double variacion,
            Duration concesion, DoubleSupplier aleatorio) {
        if (maxIntentos < 1) {
            throw new IllegalArgumentException("maxIntentos debe ser al menos 1");
        }
        if (variacion < 0 || variacion >= 1) {
            throw new IllegalArgumentException("variacion debe estar en [0, 1)");
        }
        this.maxIntentos = maxIntentos;
        this.esperaInicial = requerirPositiva(esperaInicial, "esperaInicial");
        this.esperaMaxima = requerirPositiva(esperaMaxima, "esperaMaxima");
        this.variacion = variacion;
        this.concesion = requerirPositiva(concesion, "concesion");
        this.aleatorio = Objects.requireNonNull(aleatorio, "aleatorio es obligatorio");
    }

    /** Si tras el intento número {@code intentos} todavía caben reintentos en el ciclo. */
    public boolean admiteOtroIntento(int intentos) {
        return intentos < maxIntentos;
    }

    /**
     * Espera antes del siguiente intento tras {@code intentos} fallidos: min(esperaMaxima,
     * esperaInicial × 2^(intentos−1)), con la variación aleatoria aplicada después del tope.
     */
    public Duration esperaTras(int intentos) {
        if (intentos < 1) {
            throw new IllegalArgumentException("intentos debe ser al menos 1");
        }
        // a partir de 2^30 la espera nominal ya supera cualquier tope razonable
        long nominal = intentos - 1 >= 30 ? Long.MAX_VALUE : esperaInicial.toMillis() << (intentos - 1);
        long acotada = Math.min(nominal, esperaMaxima.toMillis());
        double factor = 1 - variacion + 2 * variacion * aleatorio.getAsDouble();
        return Duration.ofMillis(Math.round(acotada * factor));
    }

    public int getMaxIntentos() {
        return maxIntentos;
    }

    public Duration getConcesion() {
        return concesion;
    }

    private static Duration requerirPositiva(Duration duracion, String nombre) {
        if (duracion == null || duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException(nombre + " debe ser positiva");
        }
        return duracion;
    }
}
