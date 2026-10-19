package com.crediticio.scoring.infrastructure.output;

import com.crediticio.scoring.ports.output.ActorActualPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Obtiene el autor del claim sub del JWT ya validado por la cadena de seguridad (HU10). No lee
 * datos enviados por el cliente: si no hay un token JWT autenticado, no hay autor.
 */
@Component
public class SecurityContextActorActualAdapter implements ActorActualPort {

    @Override
    public Optional<UUID> idUsuarioAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(jwt.getToken().getSubject()));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
