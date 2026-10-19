package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.config.JwtProperties;
import com.crediticio.config.ServicioJwtProperties;
import com.crediticio.support.ClavesRsaPrueba;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU14, identidad S1: contenido y firma del token de servicio que emite scoring-api. */
class TokenServicioJwtFirmanteTest {

    private static final KeyPair CLAVES = ClavesRsaPrueba.generar();
    private static final Instant AHORA = Instant.parse("2026-10-18T12:00:00Z");
    private static final JwtProperties JWT_USUARIOS = new JwtProperties("auth-api", "scoring-api", null);

    @TempDir
    Path directorio;

    private ServicioJwtProperties propiedades(String emisor, Duration duracion, Resource privada) {
        return new ServicioJwtProperties(emisor, "scoring-api", "evaluation-query-api-internal",
                "recomendaciones:generar", "scoring-svc+jwt", duracion, "scoring-svc-202610", privada,
                new FileSystemResource(ClavesRsaPrueba.escribirClavePublica(CLAVES, directorio)));
    }

    private Resource clavePrivada() throws Exception {
        Path archivo = directorio.resolve("clave-privada.pem");
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(CLAVES.getPrivate().getEncoded());
        Files.writeString(archivo, "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n",
                StandardCharsets.US_ASCII);
        return new FileSystemResource(archivo);
    }

    @Test
    void elTokenDebeLlevarLosClaimsYLaCabeceraAcordados() throws Exception {
        TokenServicioJwtFirmante firmante = new TokenServicioJwtFirmante(
                propiedades("scoring-api", Duration.ofSeconds(60), clavePrivada()), JWT_USUARIOS,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        SignedJWT jwt = SignedJWT.parse(firmante.emitir());

        assertThat(jwt.verify(new RSASSAVerifier((RSAPublicKey) CLAVES.getPublic()))).isTrue();
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("scoring-svc-202610");
        assertThat(jwt.getHeader().getType().getType()).isEqualTo("scoring-svc+jwt");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("scoring-api");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("scoring-api");
        assertThat(jwt.getJWTClaimsSet().getAudience()).isEqualTo(List.of("evaluation-query-api-internal"));
        assertThat(jwt.getJWTClaimsSet().getStringClaim("scope")).isEqualTo("recomendaciones:generar");
        assertThat(jwt.getJWTClaimsSet().getClaim("role")).isNull();
        assertThat(jwt.getJWTClaimsSet().getIssueTime().toInstant()).isEqualTo(AHORA);
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(AHORA.plusSeconds(60));
        assertThat(jwt.getJWTClaimsSet().getJWTID()).isNotBlank();
    }

    @Test
    void cadaLlamadaDebeEmitirUnTokenNuevo() throws Exception {
        TokenServicioJwtFirmante firmante = new TokenServicioJwtFirmante(
                propiedades("scoring-api", Duration.ofSeconds(60), clavePrivada()), JWT_USUARIOS);

        assertThat(SignedJWT.parse(firmante.emitir()).getJWTClaimsSet().getJWTID())
                .isNotEqualTo(SignedJWT.parse(firmante.emitir()).getJWTClaimsSet().getJWTID());
    }

    @Test
    void debeRechazarUnaConfiguracionInsegura() throws Exception {
        Resource privada = clavePrivada();
        assertThatThrownBy(() -> new TokenServicioJwtFirmante(propiedades("auth-api", Duration.ofSeconds(60), privada),
                JWT_USUARIOS)).isInstanceOf(IllegalStateException.class).hasMessageContaining("distinto");
        assertThatThrownBy(() -> new TokenServicioJwtFirmante(propiedades("scoring-api", Duration.ofSeconds(61),
                privada), JWT_USUARIOS)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new TokenServicioJwtFirmante(propiedades("scoring-api", Duration.ofSeconds(60),
                new FileSystemResource(directorio.resolve("no-existe.pem"))), JWT_USUARIOS))
                .isInstanceOf(IllegalStateException.class);
    }
}
