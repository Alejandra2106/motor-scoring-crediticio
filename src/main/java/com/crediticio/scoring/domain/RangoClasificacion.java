package com.crediticio.scoring.domain;

import java.util.Objects;

public class RangoClasificacion {

    private final Long idRango;
    private final CategoriaRiesgo categoria;
    private final int limiteInferior;
    private final int limiteSuperior;

    private RangoClasificacion(Long idRango, CategoriaRiesgo categoria, Integer limiteInferior,
            Integer limiteSuperior) {
        this.idRango = idRango;
        this.categoria = Objects.requireNonNull(categoria, "categoria es obligatoria");
        Objects.requireNonNull(limiteInferior, "limiteInferior es obligatorio");
        Objects.requireNonNull(limiteSuperior, "limiteSuperior es obligatorio");
        if (limiteInferior < PoliticaClasificacion.LIMITE_MINIMO
                || limiteSuperior > PoliticaClasificacion.LIMITE_MAXIMO
                || limiteInferior > limiteSuperior) {
            throw new IllegalArgumentException(
                    "Los límites del rango deben cumplir -100 <= limiteInferior <= limiteSuperior <= 130");
        }
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
    }

    static RangoClasificacion nuevo(CategoriaRiesgo categoria, Integer limiteInferior, Integer limiteSuperior) {
        return new RangoClasificacion(null, categoria, limiteInferior, limiteSuperior);
    }

    public static RangoClasificacion reconstruir(Long idRango, CategoriaRiesgo categoria, Integer limiteInferior,
            Integer limiteSuperior) {
        Objects.requireNonNull(idRango, "idRango es obligatorio");
        return new RangoClasificacion(idRango, categoria, limiteInferior, limiteSuperior);
    }

    public Long getIdRango() {
        return idRango;
    }

    public CategoriaRiesgo getCategoria() {
        return categoria;
    }

    public int getLimiteInferior() {
        return limiteInferior;
    }

    public int getLimiteSuperior() {
        return limiteSuperior;
    }

    /** Límites inclusivos. */
    public boolean contiene(int puntaje) {
        return limiteInferior <= puntaje && puntaje <= limiteSuperior;
    }
}
