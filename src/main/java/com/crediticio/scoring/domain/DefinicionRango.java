package com.crediticio.scoring.domain;

/**
 * Datos propuestos para un rango, todavía sin validar. La política los valida en conjunto
 * (cobertura, huecos y solapamientos) antes de convertirlos en {@link RangoClasificacion}.
 */
public record DefinicionRango(CategoriaRiesgo categoria, Integer limiteInferior, Integer limiteSuperior) {
}
