package com.crediticio.config;

import com.crediticio.support.ClavesRsaPrueba;
import com.crediticio.support.TokensPrueba;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU10 CA01, CA06 y CA08: validación local del contrato JWT de auth-api con claves efímeras. */
class JwtConfigTest {

    private static final String EMISOR = "auth-api";
    private static final KeyPair CLAVES = ClavesRsaPrueba.generar();
    private static final KeyPair OTRAS_CLAVES = ClavesRsaPrueba.generar();
    private static final JwtDecoder DECODER =
            JwtConfig.decodificador((RSAPublicKey) CLAVES.getPublic(), EMISOR, "scoring-api");

    private static String firmado(JWTClaimsSet claims) {
        return TokensPrueba.firmar(CLAVES, claims);
    }

    private static JWTClaimsSet.Builder validos() {
        return TokensPrueba.claimsValidos(EMISOR, JwtConfig.ROL_ANALISTA);
    }

    @Test
    void debeAceptarUnTokenValidoDeAuthApi() {
        Jwt jwt = DECODER.decode(firmado(validos().build()));

        assertThat(jwt.getClaimAsString("role")).isEqualTo(JwtConfig.ROL_ANALISTA);
        assertThat(jwt.getAudience()).contains("scoring-api");
    }

    @Test
    void debeRechazarUnTokenFirmadoConOtraClave() {
        String token = TokensPrueba.firmar(OTRAS_CLAVES, validos().build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnTokenHs256() throws Exception {
        SignedJWT hs256 = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), validos().build());
        hs256.sign(new MACSigner("secreto-de-prueba-de-al-menos-32-bytes!!".getBytes()));

        assertThatThrownBy(() -> DECODER.decode(hs256.serialize())).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnTokenSinFirma() {
        String sinFirma = new PlainJWT(validos().build()).serialize();

        assertThatThrownBy(() -> DECODER.decode(sinFirma)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarOtroEmisor() {
        String token = firmado(validos().issuer("otro-emisor").build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnaAudienciaQueNoIncluyeScoringApi() {
        String token = firmado(validos().audience(List.of("auth-api", "evaluation-query-api")).build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnTokenExpiradoFueraDeLaToleranciaDeReloj() {
        Instant hace = Instant.now().minus(Duration.ofMinutes(20));
        String token = firmado(validos().issueTime(Date.from(hace))
                .expirationTime(Date.from(Instant.now().minusSeconds(61))).build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeAceptarUnTokenRecienExpiradoDentroDeLaToleranciaDeReloj() {
        Instant hace = Instant.now().minus(Duration.ofMinutes(15));
        String token = firmado(validos().issueTime(Date.from(hace))
                .expirationTime(Date.from(Instant.now().minusSeconds(30))).build());

        assertThat(DECODER.decode(token).getSubject()).isNotBlank();
    }

    @Test
    void debeRechazarUnTokenSinExp() {
        String token = firmado(validos().expirationTime(null).build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnTokenSinSub() {
        String token = firmado(validos().subject(null).build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnSubQueNoEsUuid() {
        String token = firmado(validos().subject("usuario-123").build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnTokenSinRole() {
        String token = firmado(validos().claim("role", null).build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void debeRechazarUnRoleQueNoEsTexto() {
        String token = firmado(validos().claim("role", List.of(JwtConfig.ROL_ADMINISTRADOR)).build());

        assertThatThrownBy(() -> DECODER.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void elRolPermitidoDelTokenDebeConvertirseEnSuAutoridad() {
        Jwt jwt = DECODER.decode(firmado(TokensPrueba.claimsValidos(EMISOR, JwtConfig.ROL_ADMINISTRADOR).build()));

        AbstractAuthenticationToken autenticacion = JwtConfig.convertidorAutenticacion().convert(jwt);

        assertThat(rolesDe(autenticacion)).containsExactly("ROLE_ADMINISTRADOR_RIESGO");
        assertThat(autenticacion.getName()).isEqualTo(jwt.getSubject());
    }

    @Test
    void unRolDesconocidoNoDebeOtorgarRoles() {
        Jwt jwt = DECODER.decode(firmado(TokensPrueba.claimsValidos(EMISOR, "AUDITOR").build()));

        AbstractAuthenticationToken autenticacion = JwtConfig.convertidorAutenticacion().convert(jwt);

        assertThat(rolesDe(autenticacion)).isEmpty();
    }

    /** Spring Security 7 añade además la autoridad de factor FACTOR_BEARER; aquí solo interesan los roles. */
    private static List<String> rolesDe(AbstractAuthenticationToken autenticacion) {
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith("ROLE_"))
                .toList();
    }

    @Test
    void sinEmisorConfiguradoNoDebeConstruirseElValidador() {
        assertThatThrownBy(() -> JwtConfig.validador(" ", "scoring-api")).isInstanceOf(IllegalStateException.class);
    }
}
