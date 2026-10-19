package com.crediticio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * Token de servicio de scoring-api hacia evaluation-query-api (HU14, identidad S1). Par RS256 propio de
 * scoring-api: la clave privada solo existe aquí (archivo PEM PKCS#8 externo, nunca versionado); la pública se
 * entrega a evaluation-query-api. El emisor debe ser distinto del de auth-api.
 */
@ConfigurationProperties("scoring.servicio-jwt")
public record ServicioJwtProperties(
        String emisor,
        String sujeto,
        String audiencia,
        String permiso,
        String tipo,
        Duration duracion,
        String idClave,
        Resource clavePrivada,
        Resource clavePublica) {
}
