package com.crediticio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * HU10: validación local, sin llamar a auth-api, del contrato JWT común emitido por auth-api:
 * firma RS256 con su clave pública, emisor, audiencia que incluya esta API, vigencia (exp
 * obligatorio, 60 s de tolerancia de reloj), sub con formato UUID y claim role de texto. Si la clave
 * pública falta o es inválida, la aplicación no arranca.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    public static final String CLAIM_ROL = "role";
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR_RIESGO";
    public static final String ROL_ANALISTA = "ANALISTA_CREDITO";
    private static final Set<String> ROLES_PERMITIDOS = Set.of(ROL_ADMINISTRADOR, ROL_ANALISTA);

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties propiedades) {
        return decodificador(leerClavePublica(propiedades.clavePublica()), propiedades.emisor(),
                propiedades.audiencia());
    }

    /** Solo acepta RS256: un token con otro algoritmo (HS256, none) se rechaza como inválido. */
    public static JwtDecoder decodificador(RSAPublicKey clavePublica, String emisor, String audiencia) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(clavePublica)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validador(emisor, audiencia));
        return decoder;
    }

    /**
     * El validador por defecto de Spring acepta tokens sin exp; aquí exp, sub y role son
     * obligatorios. Un role desconocido no invalida el token: se autentica sin permisos (403).
     */
    public static OAuth2TokenValidator<Jwt> validador(String emisor, String audiencia) {
        requerirTexto(emisor, "scoring.jwt.emisor");
        requerirTexto(audiencia, "scoring.jwt.audiencia");
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(emisor),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        audiencias -> audiencias != null && audiencias.contains(audiencia)),
                new JwtClaimValidator<Instant>(JwtClaimNames.EXP, exp -> exp != null),
                new JwtClaimValidator<String>(JwtClaimNames.SUB, JwtConfig::esUuid),
                new JwtClaimValidator<Object>(CLAIM_ROL, rol -> rol instanceof String texto && !texto.isBlank()));
    }

    /** El rol sale únicamente del claim role del token ya validado; solo los roles del modelo dan permisos. */
    public static JwtAuthenticationConverter convertidorAutenticacion() {
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(JwtConfig::autoridades);
        return convertidor;
    }

    private static Collection<GrantedAuthority> autoridades(Jwt jwt) {
        String rol = jwt.getClaimAsString(CLAIM_ROL);
        if (rol == null || !ROLES_PERMITIDOS.contains(rol)) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static boolean esUuid(String valor) {
        if (valor == null) {
            return false;
        }
        try {
            return UUID.fromString(valor).toString().equalsIgnoreCase(valor);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static void requerirTexto(String valor, String propiedad) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la propiedad " + propiedad);
        }
    }

    private static RSAPublicKey leerClavePublica(Resource recurso) {
        if (recurso == null || !recurso.exists()) {
            throw new IllegalStateException("No se encontró la clave pública del JWT configurada");
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return RsaKeyConverters.x509().convert(entrada);
        } catch (IOException ex) {
            throw new UncheckedIOException("No fue posible leer la clave pública del JWT", ex);
        }
    }
}
