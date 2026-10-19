package com.crediticio.evaluations.infrastructure.output;

import com.crediticio.config.SagaRecomendacionesProperties;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import com.crediticio.evaluations.ports.output.ResultadoGeneracion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cliente de la operación interna de HU14 contra un servidor HTTP local del JDK. Incluye la prueba B: el límite
 * total de la llamada se aplica también cuando el cuerpo llega poco a poco (no es un tiempo entre lecturas).
 * Límites reducidos a 1 s para que la prueba sea rápida.
 */
class GeneracionRecomendacionesHttpAdapterTest {

    /** Límite de las pruebas de tiempo. */
    private static final Duration LIMITE = Duration.ofSeconds(1);
    /** Límite amplio para las demás: el primer uso en una JVM fría puede superar 1 s. */
    private static final Duration LIMITE_AMPLIO = Duration.ofSeconds(5);

    private HttpServer servidor;
    private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
    private final AtomicReference<String> metodoYRutaRecibidos = new AtomicReference<>();
    private volatile Respuesta respuesta;

    @FunctionalInterface
    private interface Respuesta {
        void escribir(HttpExchange intercambio) throws Exception;
    }

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/", intercambio -> {
            autorizacionRecibida.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            metodoYRutaRecibidos.set(intercambio.getRequestMethod() + " " + intercambio.getRequestURI().getPath());
            try {
                respuesta.escribir(intercambio);
            } catch (Exception ex) {
                // el cliente cerró la conexión al vencer su límite
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private GeneracionRecomendacionesHttpAdapter adaptador(String urlBase, Duration limite) {
        SagaRecomendacionesProperties propiedades = new SagaRecomendacionesProperties(true, urlBase, 10,
                Duration.ofSeconds(5), Duration.ofSeconds(300), 0.2, Duration.ofSeconds(30), limite, limite, limite,
                limite, 20);
        TokenServicioJwtFirmante firmante = mock(TokenServicioJwtFirmante.class);
        when(firmante.emitir()).thenReturn("token-de-servicio");
        return new GeneracionRecomendacionesHttpAdapter(propiedades, firmante);
    }

    private GeneracionRecomendacionesHttpAdapter adaptador() {
        return adaptador("http://127.0.0.1:" + servidor.getAddress().getPort(), LIMITE_AMPLIO);
    }

    private GeneracionRecomendacionesHttpAdapter adaptadorConLimiteCorto() {
        return adaptador("http://127.0.0.1:" + servidor.getAddress().getPort(), LIMITE);
    }

    private static Respuesta json(int estado, String cuerpo) {
        return intercambio -> {
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        };
    }

    @Test
    void unaRespuestaValidaDebeInterpretarseComoExitoConElTokenDeServicio() {
        respuesta = json(201, "{\"idEvaluacion\":7,\"versionCatalogo\":\"2026.1\",\"cantidad\":2}");

        ResultadoGeneracion resultado = adaptador().generar(7, ModoIntento.EN_LINEA);

        assertThat(resultado).isEqualTo(new ResultadoGeneracion.Exito("2026.1", 2));
        assertThat(autorizacionRecibida.get()).isEqualTo("Bearer token-de-servicio");
        assertThat(metodoYRutaRecibidos.get()).isEqualTo("PUT /internal/v1/evaluaciones/7/recomendaciones");
    }

    @Test
    void unaRespuestaQueNoConfirmaLaEvaluacionDebeSerUnErrorPermanente() {
        respuesta = json(200, "{\"idEvaluacion\":8,\"versionCatalogo\":\"2026.1\",\"cantidad\":2}");

        assertThat(adaptador().generar(7, ModoIntento.REENVIO))
                .isInstanceOfSatisfying(ResultadoGeneracion.ErrorPermanente.class,
                        error -> assertThat(error.codigo()).isEqualTo("CONTRATO_INVALIDO"));
    }

    @Test
    void elCatalogoNoDisponibleDebeSerTransitorio() {
        respuesta = json(503, "{\"errorCode\":\"CATALOGO_NO_DISPONIBLE\",\"message\":\"x\",\"details\":[],\"traceId\":null}");

        assertThat(adaptador().generar(7, ModoIntento.REENVIO))
                .isInstanceOfSatisfying(ResultadoGeneracion.ErrorTransitorio.class,
                        error -> assertThat(error.codigo()).isEqualTo("CATALOGO_NO_DISPONIBLE"));
    }

    @Test
    void unErrorDelServidorDebeSerTransitorio() {
        respuesta = json(500, "{\"errorCode\":\"INTERNAL_ERROR\"}");

        assertThat(adaptador().generar(7, ModoIntento.REENVIO))
                .isInstanceOfSatisfying(ResultadoGeneracion.ErrorTransitorio.class,
                        error -> assertThat(error.codigo()).isEqualTo("ERROR_REMOTO"));
    }

    @Test
    void laIdentidadRechazadaYLosRechazosFuncionalesDebenSerPermanentes() {
        respuesta = json(401, "{\"errorCode\":\"NO_AUTENTICADO\"}");
        assertThat(adaptador().generar(7, ModoIntento.REENVIO))
                .isInstanceOfSatisfying(ResultadoGeneracion.ErrorPermanente.class,
                        error -> assertThat(error.codigo()).isEqualTo("IDENTIDAD_RECHAZADA"));

        respuesta = json(409, "{\"errorCode\":\"EVALUACION_NO_PENDIENTE\"}");
        assertThat(adaptador().generar(7, ModoIntento.REENVIO))
                .isInstanceOfSatisfying(ResultadoGeneracion.ErrorPermanente.class,
                        error -> assertThat(error.codigo()).isEqualTo("RECHAZO_FUNCIONAL"));
    }

    @Test
    void sinConexionDebeSerTransitorio() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puertoLibre = socket.getLocalPort();
        }

        assertThat(adaptador("http://127.0.0.1:" + puertoLibre, LIMITE_AMPLIO).generar(7, ModoIntento.EN_LINEA))
                .isInstanceOfSatisfying(ResultadoGeneracion.ErrorTransitorio.class,
                        error -> assertThat(error.codigo()).isEqualTo("SIN_CONEXION"));
    }

    @Test
    void unasCabecerasQueTardanDebenCortarseAlVencerElLimite() {
        respuesta = intercambio -> {
            Thread.sleep(3000);
            json(200, "{}").escribir(intercambio);
        };

        long inicio = System.nanoTime();
        ResultadoGeneracion resultado = adaptadorConLimiteCorto().generar(7, ModoIntento.EN_LINEA);
        Duration transcurrido = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(resultado).isInstanceOfSatisfying(ResultadoGeneracion.ErrorTransitorio.class,
                error -> assertThat(error.codigo()).isEqualTo("TIEMPO_AGOTADO"));
        assertThat(transcurrido).isLessThan(Duration.ofMillis(2500));
    }

    /** Prueba B: cabeceras inmediatas y un byte cada 200 ms durante 5 s; el límite total (1 s) debe cortarla. */
    @Test
    void unCuerpoQueLlegaPocoAPocoDebeCortarseAlVencerElLimiteTotal() {
        respuesta = intercambio -> {
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, 0);
            try (OutputStream salida = intercambio.getResponseBody()) {
                for (int i = 0; i < 25; i++) {
                    salida.write(' ');
                    salida.flush();
                    Thread.sleep(200);
                }
                salida.write("{\"idEvaluacion\":7,\"versionCatalogo\":\"2026.1\",\"cantidad\":1}".getBytes());
            }
        };

        long inicio = System.nanoTime();
        ResultadoGeneracion resultado = adaptadorConLimiteCorto().generar(7, ModoIntento.EN_LINEA);
        Duration transcurrido = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(resultado).isInstanceOfSatisfying(ResultadoGeneracion.ErrorTransitorio.class,
                error -> assertThat(error.codigo()).isEqualTo("TIEMPO_AGOTADO"));
        assertThat(transcurrido).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    void unCuerpoLentoPeroDentroDelLimiteDebeAceptarse() {
        respuesta = intercambio -> {
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, 0);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write("{\"idEvaluacion\":7,".getBytes());
                salida.flush();
                Thread.sleep(400);
                salida.write("\"versionCatalogo\":\"2026.1\",\"cantidad\":1}".getBytes());
            }
        };

        assertThat(adaptador().generar(7, ModoIntento.EN_LINEA)).isEqualTo(new ResultadoGeneracion.Exito("2026.1", 1));
    }
}
