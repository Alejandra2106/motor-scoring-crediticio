package com.crediticio.scoring.ports.output;

import java.util.Optional;
import java.util.UUID;

/**
 * Identidad del usuario autenticado que ejecuta la operación (claim sub del JWT validado, HU10).
 * Vacío cuando no hay un usuario autenticado; nunca devuelve un identificador predeterminado.
 */
public interface ActorActualPort {

    Optional<UUID> idUsuarioAutenticado();
}
