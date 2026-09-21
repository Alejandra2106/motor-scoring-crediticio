package com.crediticio.evaluations.ports.output;

public record ResultadoReglaEvaluada(
        Long idRegla,
        String operadorAplicado,
        String valorCondicionAplicado,
        Integer puntajeReglaAplicado,
        boolean condicionCumplida,
        Integer puntajeObtenido) {
}
