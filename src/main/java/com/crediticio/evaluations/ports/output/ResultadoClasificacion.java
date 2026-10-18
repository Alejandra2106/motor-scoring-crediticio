package com.crediticio.evaluations.ports.output;

/**
 * Resultado de clasificar un puntaje con una versión concreta de la política. {@code categoria} es
 * null cuando el puntaje no pertenece a ningún rango de esa política.
 */
public record ResultadoClasificacion(Long idPolitica, Integer versionPolitica, String categoria) {
}
