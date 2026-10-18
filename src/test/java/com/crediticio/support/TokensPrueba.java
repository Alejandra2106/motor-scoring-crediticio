package com.crediticio.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Tokens de prueba con el mismo contrato que emite auth-api (JwtEmisorTokenAccesoAdapter): RS256
 * con kid; iss, aud (lista de las tres API), sub (UUID), role, iat, exp (15 minutos) y jti.
 */
public final class TokensPrueba {

    public static final List<String> AUDIENCIAS = List.of("auth-api", "scoring-api", "evaluation-query-api");

    private TokensPrueba() {
    }

    public static JWTClaimsSet.Builder claimsValidos(String emisor, String rol) {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(emisor)
                .audience(AUDIENCIAS)
                .subject(UUID.randomUUID().toString())
                .claim("role", rol)
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plus(Duration.ofMinutes(15))))
                .jwtID(UUID.randomUUID().toString());
    }

    public static String firmar(KeyPair par, JWTClaimsSet claims) {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("clave-prueba").build(), claims);
        try {
            jwt.sign(new RSASSASigner(par.getPrivate()));
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
        return jwt.serialize();
    }
}
