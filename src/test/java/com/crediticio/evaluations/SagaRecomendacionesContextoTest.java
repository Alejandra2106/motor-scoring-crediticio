package com.crediticio.evaluations;

import com.crediticio.evaluations.application.EvaluarSolicitanteService;
import com.crediticio.evaluations.infrastructure.input.ReenvioRecomendacionesProgramado;
import com.crediticio.evaluations.infrastructure.output.GeneracionRecomendacionesHttpAdapter;
import com.crediticio.evaluations.infrastructure.output.TokenServicioJwtFirmante;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort;
import com.crediticio.support.ClavesRsaPrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU14: con scoring.saga.habilitada=true el contexto completo arranca y conecta los componentes reales de la
 * saga (firmante S1, cliente HTTP, intento en línea y reenvío programado). Claves RSA efímeras; la URL de
 * evaluation-query-api no se llama y el reenvío no llega a ejecutarse (frecuencia de una hora).
 */
@SpringBootTest(properties = {
        "scoring.saga.habilitada=true",
        "scoring.saga.url-base=http://localhost:1",
        "scoring.saga.frecuencia-reenvio=PT1H",
        "scoring.servicio-jwt.id-clave=scoring-svc-prueba"})
@ActiveProfiles("test")
@DirtiesContext
class SagaRecomendacionesContextoTest {

    private static final KeyPair CLAVES_USUARIOS = ClavesRsaPrueba.generar();
    private static final KeyPair CLAVES_SERVICIO = ClavesRsaPrueba.generar();

    @DynamicPropertySource
    static void configurarClaves(DynamicPropertyRegistry registro) throws IOException {
        Path directorio = Files.createTempDirectory("saga-contexto");
        directorio.toFile().deleteOnExit();
        registro.add("scoring.jwt.clave-publica", () -> "file:" + ClavesRsaPrueba.escribirClavePublica(
                CLAVES_USUARIOS, subdirectorio(directorio, "usuarios")));
        registro.add("scoring.servicio-jwt.clave-publica", () -> "file:" + ClavesRsaPrueba.escribirClavePublica(
                CLAVES_SERVICIO, subdirectorio(directorio, "servicio")));
        registro.add("scoring.servicio-jwt.clave-privada", () -> "file:" + escribirClavePrivada(directorio));
    }

    @Autowired
    private ApplicationContext contexto;

    @Test
    void laSagaHabilitadaUsaLosAdaptadoresReales() {
        assertThat(contexto.getBean(CalcularScoreUseCase.class)).isInstanceOf(EvaluarSolicitanteService.class);
        assertThat(contexto.getBean(GeneracionRecomendacionesPort.class))
                .isInstanceOf(GeneracionRecomendacionesHttpAdapter.class);
        assertThat(contexto.getBean(TokenServicioJwtFirmante.class).emitir()).isNotBlank();
        assertThat(contexto.getBeansOfType(ReenvioRecomendacionesProgramado.class)).hasSize(1);
    }

    private static Path subdirectorio(Path directorio, String nombre) {
        try {
            return Files.createDirectories(directorio.resolve(nombre));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Path escribirClavePrivada(Path directorio) {
        Path archivo = subdirectorio(directorio, "servicio").resolve("clave-privada.pem");
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(CLAVES_SERVICIO.getPrivate().getEncoded());
        try {
            Files.writeString(archivo, "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n",
                    StandardCharsets.US_ASCII);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return archivo;
    }
}
