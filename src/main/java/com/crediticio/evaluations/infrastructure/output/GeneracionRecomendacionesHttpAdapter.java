package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.config.SagaRecomendacionesProperties;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort;
import com.crediticio.evaluations.ports.output.ResultadoGeneracion;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.UUID;

/**
 * Cliente de la operación interna PUT /internal/v1/evaluaciones/{id}/recomendaciones de evaluation-query-api
 * (HU14). Cada llamada lleva un token de servicio nuevo y tiene un límite total: el tiempo de lectura de
 * JdkClientHttpRequestFactory cancela la petición y cierra el cuerpo al vencer, aunque los datos sigan
 * llegando poco a poco. Clasifica el resultado en transitorio o permanente para la política de reintentos.
 */
@Component
@ConditionalOnProperty(prefix = "scoring.saga", name = "habilitada", havingValue = "true")
public class GeneracionRecomendacionesHttpAdapter implements GeneracionRecomendacionesPort {

    static final String RUTA = "/internal/v1/evaluaciones/{idEvaluacion}/recomendaciones";

    private final RestClient clienteEnLinea;
    private final RestClient clienteReenvio;
    private final Duration limiteEnLinea;
    private final Duration limiteReenvio;
    private final TokenServicioJwtFirmante firmante;

    public GeneracionRecomendacionesHttpAdapter(SagaRecomendacionesProperties propiedades,
            TokenServicioJwtFirmante firmante) {
        if (propiedades.urlBase() == null || propiedades.urlBase().isBlank()) {
            throw new IllegalStateException("Falta la propiedad scoring.saga.url-base");
        }
        this.clienteEnLinea = cliente(propiedades.urlBase(), propiedades.conexionEnLinea(), propiedades.limiteEnLinea());
        this.clienteReenvio = cliente(propiedades.urlBase(), propiedades.conexionReenvio(), propiedades.limiteReenvio());
        this.limiteEnLinea = propiedades.limiteEnLinea();
        this.limiteReenvio = propiedades.limiteReenvio();
        this.firmante = firmante;
    }

    static RestClient cliente(String urlBase, Duration conexion, Duration limiteTotal) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(conexion).build());
        fabrica.setReadTimeout(limiteTotal);
        return RestClient.builder().requestFactory(fabrica).baseUrl(urlBase).build();
    }

    @Override
    public ResultadoGeneracion generar(long idEvaluacion, ModoIntento modo) {
        String token;
        try {
            token = firmante.emitir();
        } catch (RuntimeException ex) {
            return new ResultadoGeneracion.ErrorPermanente("CONFIGURACION_LOCAL", "No se pudo firmar el token de servicio");
        }
        RestClient cliente = modo == ModoIntento.EN_LINEA ? clienteEnLinea : clienteReenvio;
        Duration limite = modo == ModoIntento.EN_LINEA ? limiteEnLinea : limiteReenvio;
        long inicio = System.nanoTime();
        try {
            ResponseEntity<RespuestaGeneracion> respuesta = cliente.put()
                    .uri(RUTA, idEvaluacion)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-Trace-Id", traceId())
                    .retrieve()
                    .toEntity(RespuestaGeneracion.class);
            return interpretar(idEvaluacion, respuesta.getBody());
        } catch (HttpClientErrorException ex) {
            return errorCliente(ex.getStatusCode());
        } catch (HttpServerErrorException ex) {
            String cuerpo = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 503 && cuerpo.contains("CATALOGO_NO_DISPONIBLE")) {
                return new ResultadoGeneracion.ErrorTransitorio("CATALOGO_NO_DISPONIBLE",
                        "El catálogo de recomendaciones no está disponible o no está aprobado");
            }
            return new ResultadoGeneracion.ErrorTransitorio("ERROR_REMOTO", "HTTP " + ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            if (tieneCausa(ex, HttpTimeoutException.class)) {
                return new ResultadoGeneracion.ErrorTransitorio("TIEMPO_AGOTADO", "Se superó el límite de la llamada");
            }
            return new ResultadoGeneracion.ErrorTransitorio("SIN_CONEXION", "No se pudo conectar con evaluation-query-api");
        } catch (RestClientException ex) {
            // Si el límite total vence mientras se lee el cuerpo, la petición se cancela y el flujo se cierra:
            // llega como un error de extracción con una IOException, sin HttpTimeoutException.
            if (tieneCausa(ex, HttpTimeoutException.class)
                    || tieneCausa(ex, IOException.class) && Duration.ofNanos(System.nanoTime() - inicio).compareTo(limite) >= 0) {
                return new ResultadoGeneracion.ErrorTransitorio("TIEMPO_AGOTADO", "Se superó el límite de la llamada");
            }
            if (tieneCausa(ex, IOException.class)) {
                return new ResultadoGeneracion.ErrorTransitorio("SIN_CONEXION", "Se interrumpió la respuesta");
            }
            return new ResultadoGeneracion.ErrorPermanente("CONTRATO_INVALIDO", "Respuesta no interpretable");
        }
    }

    private static ResultadoGeneracion interpretar(long idEvaluacion, RespuestaGeneracion cuerpo) {
        if (cuerpo == null || cuerpo.idEvaluacion() == null || cuerpo.idEvaluacion() != idEvaluacion
                || cuerpo.versionCatalogo() == null || cuerpo.versionCatalogo().isBlank()
                || cuerpo.cantidad() == null || cuerpo.cantidad() < 0) {
            return new ResultadoGeneracion.ErrorPermanente("CONTRATO_INVALIDO",
                    "La respuesta no confirma la generación de la evaluación " + idEvaluacion);
        }
        return new ResultadoGeneracion.Exito(cuerpo.versionCatalogo(), cuerpo.cantidad());
    }

    private static ResultadoGeneracion errorCliente(HttpStatusCode estado) {
        int codigo = estado.value();
        if (codigo == 401 || codigo == 403) {
            return new ResultadoGeneracion.ErrorPermanente("IDENTIDAD_RECHAZADA", "HTTP " + codigo);
        }
        if (codigo == 429) {
            return new ResultadoGeneracion.ErrorTransitorio("LIMITADO", "HTTP 429");
        }
        return new ResultadoGeneracion.ErrorPermanente("RECHAZO_FUNCIONAL", "HTTP " + codigo);
    }

    private static boolean tieneCausa(Throwable error, Class<? extends Throwable> tipo) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (tipo.isInstance(causa)) {
                return true;
            }
        }
        return false;
    }

    private static String traceId() {
        String actual = MDC.get("traceId");
        return actual != null ? actual : UUID.randomUUID().toString();
    }

    /** Cuerpo de la respuesta 200/201 de la operación interna. */
    record RespuestaGeneracion(Long idEvaluacion, String versionCatalogo, Integer cantidad) {
    }
}
