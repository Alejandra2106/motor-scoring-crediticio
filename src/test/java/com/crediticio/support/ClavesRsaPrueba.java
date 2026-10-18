package com.crediticio.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Par de claves RSA generado en tiempo de ejecución para las pruebas, igual que en auth-api. Ninguna
 * clave se guarda en el repositorio: la pública se escribe en un directorio temporal de JUnit y la
 * privada solo existe en memoria para firmar los tokens de prueba.
 */
public final class ClavesRsaPrueba {

    private ClavesRsaPrueba() {
    }

    public static KeyPair generar() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Escribe clave-publica.pem (X.509) en el directorio indicado y devuelve su ruta. */
    public static Path escribirClavePublica(KeyPair par, Path directorio) {
        Path archivo = directorio.resolve("clave-publica.pem");
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(par.getPublic().getEncoded());
        try {
            Files.writeString(archivo, "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n",
                    StandardCharsets.US_ASCII);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return archivo;
    }
}
