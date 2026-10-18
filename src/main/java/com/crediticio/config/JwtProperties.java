package com.crediticio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Validación local de los JWT de acceso emitidos por auth-api (HU10). Solo se configura la clave
 * pública (archivo PEM X.509 externo, p. ej. file:/ruta/jwt-publica.pem); la clave privada nunca
 * sale de auth-api.
 */
@ConfigurationProperties("scoring.jwt")
public record JwtProperties(
        String emisor,
        String audiencia,
        Resource clavePublica) {
}
