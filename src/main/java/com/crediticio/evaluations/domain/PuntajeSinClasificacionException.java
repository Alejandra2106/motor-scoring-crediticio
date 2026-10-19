package com.crediticio.evaluations.domain;

/** HU12 CA04: el puntaje total no pertenece a ningún rango de la política vigente; la evaluación se rechaza. */
public class PuntajeSinClasificacionException extends RuntimeException {

    public PuntajeSinClasificacionException(int puntajeTotal) {
        super("El puntaje total " + puntajeTotal + " no pertenece a ningún rango de la política de clasificación "
                + "vigente; la evaluación no se registró");
    }
}
