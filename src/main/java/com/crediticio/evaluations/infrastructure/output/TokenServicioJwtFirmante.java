package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.config.JwtProperties;
import com.crediticio.config.ServicioJwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Emite el token de servicio de scoring-api (HU14, identidad S1): RS256 con kid, typ propio, iss y sub del
 * servicio, aud de la ruta interna de evaluation-query-api, iat, exp de vida corta (60 s), jti y el permiso
 * en el claim scope; sin claim role, para que nunca autorice operaciones de usuario. Un token nuevo por
 * llamada. Si falta o es inválida alguna clave, la aplicación no arranca.
 */
@Component
@ConditionalOnProperty(prefix = "scoring.saga", name = "habilitada", havingValue = "true")
public class TokenServicioJwtFirmante {

    private static final Duration DURACION_MAXIMA = Duration.ofSeconds(60);

    private final JwtEncoder encoder;
    private final ServicioJwtProperties propiedades;
    private final Clock reloj;

    // Con dos constructores, Spring necesita saber cuál usar; el otro solo inyecta el reloj en las pruebas
    @Autowired
    public TokenServicioJwtFirmante(ServicioJwtProperties propiedades, JwtProperties jwtUsuarios) {
        this(propiedades, jwtUsuarios, Clock.systemUTC());
    }

    TokenServicioJwtFirmante(ServicioJwtProperties propiedades, JwtProperties jwtUsuarios, Clock reloj) {
        requerirTexto(propiedades.emisor(), "scoring.servicio-jwt.emisor");
        requerirTexto(propiedades.sujeto(), "scoring.servicio-jwt.sujeto");
        requerirTexto(propiedades.audiencia(), "scoring.servicio-jwt.audiencia");
        requerirTexto(propiedades.permiso(), "scoring.servicio-jwt.permiso");
        requerirTexto(propiedades.tipo(), "scoring.servicio-jwt.tipo");
        requerirTexto(propiedades.idClave(), "scoring.servicio-jwt.id-clave");
        if (propiedades.emisor().equals(jwtUsuarios.emisor())) {
            throw new IllegalStateException("El emisor del token de servicio debe ser distinto del de auth-api");
        }
        if (propiedades.duracion() == null || propiedades.duracion().isNegative() || propiedades.duracion().isZero()
                || propiedades.duracion().compareTo(DURACION_MAXIMA) > 0) {
            throw new IllegalStateException("scoring.servicio-jwt.duracion debe estar entre 1 s y 60 s");
        }
        RSAKey clave = new RSAKey.Builder(leer(propiedades.clavePublica(), RsaKeyConverters.x509()::convert, "pública"))
                .privateKey(leer(propiedades.clavePrivada(), RsaKeyConverters.pkcs8()::convert, "privada"))
                .keyID(propiedades.idClave())
                .keyUse(KeyUse.SIGNATURE)
                .build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public String emitir() {
        Instant ahora = Instant.now(reloj);
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(propiedades.idClave())
                .type(propiedades.tipo())
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(propiedades.sujeto())
                .audience(List.of(propiedades.audiencia()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.duracion()))
                .id(UUID.randomUUID().toString())
                .claim("scope", propiedades.permiso())
                .build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    private static void requerirTexto(String valor, String propiedad) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la propiedad " + propiedad);
        }
    }

    private static <T> T leer(Resource recurso, Function<InputStream, T> convertidor, String tipo) {
        if (recurso == null || !recurso.exists()) {
            throw new IllegalStateException("No se encontró la clave " + tipo + " del token de servicio");
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return convertidor.apply(entrada);
        } catch (IOException ex) {
            throw new UncheckedIOException("No fue posible leer la clave " + tipo + " del token de servicio", ex);
        }
    }
}
