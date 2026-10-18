package com.crediticio;

import com.crediticio.support.ClavesRsaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@SpringBootTest
@ActiveProfiles("test")
class CreditScoringEngineApplicationTests {

    @TempDir
    static Path claves;

    /** HU10: la validación JWT exige una clave pública; se genera una efímera en tiempo de ejecución. */
    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("scoring.jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(ClavesRsaPrueba.generar(), claves));
    }

    @Test
    void contextLoads() {
    }
}
