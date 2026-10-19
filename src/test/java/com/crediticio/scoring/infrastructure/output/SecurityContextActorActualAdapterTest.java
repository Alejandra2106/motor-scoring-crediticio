package com.crediticio.scoring.infrastructure.output;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU11 CA14: el autor sale solo del claim sub de un JWT autenticado; nunca se inventa. */
class SecurityContextActorActualAdapterTest {

    private final SecurityContextActorActualAdapter adapter = new SecurityContextActorActualAdapter();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarConJwt(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_RIESGO"))));
    }

    @Test
    void debeRetornarElSubDelJwtAutenticado() {
        UUID usuario = UUID.randomUUID();
        autenticarConJwt(usuario.toString());

        assertThat(adapter.idUsuarioAutenticado()).contains(usuario);
    }

    @Test
    void sinAutenticacionNoDebeHaberAutor() {
        assertThat(adapter.idUsuarioAutenticado()).isEmpty();
    }

    @Test
    void unUsuarioAnonimoNoDebeSerAutor() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(adapter.idUsuarioAutenticado()).isEmpty();
    }

    @Test
    void unaAutenticacionQueNoEsJwtNoDebeAportarAutor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_RIESGO"))));

        assertThat(adapter.idUsuarioAutenticado()).isEmpty();
    }

    @Test
    void unSubQueNoEsUuidNoDebeAportarAutor() {
        autenticarConJwt("usuario-123");

        assertThat(adapter.idUsuarioAutenticado()).isEmpty();
    }
}
