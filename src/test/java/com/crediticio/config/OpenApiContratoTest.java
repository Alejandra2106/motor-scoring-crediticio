package com.crediticio.config;

import com.crediticio.support.ClavesRsaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato OpenAPI de scoring-api con springdoc habilitado (como en el perfil dev): exactamente las
 * operaciones implementadas, sin rutas duplicadas ni inesperadas, esquema de seguridad JWT y
 * respuestas 401/403 documentadas en todas ellas (HU10).
 */
@SpringBootTest(properties = {"springdoc.api-docs.enabled=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContratoTest {

    private static final Map<String, String> OPERACIONES = Map.of(
            "/api/v1/solicitantes", "post",
            "/api/v1/solicitantes/documento/{numeroDocumento}", "get",
            "/api/v1/variables-riesgo", "post",
            "/api/v1/variables-riesgo/{idRiesgo}/estado", "patch",
            "/api/v1/reglas-scoring", "post",
            "/api/v1/reglas-scoring/{idRegla}", "patch",
            "/api/v1/evaluaciones", "post",
            "/api/v1/evaluaciones/{idEvaluacion}", "get",
            "/api/v1/politicas-clasificacion", "post",
            "/api/v1/politicas-clasificacion/vigente", "get");

    @TempDir
    static Path claves;

    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("scoring.jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(ClavesRsaPrueba.generar(), claves));
    }

    @Autowired
    private MockMvc mockMvc;

    private ResultActions contrato() throws Exception {
        return mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void debeDocumentarExactamenteLasOperacionesImplementadas() throws Exception {
        ResultActions contrato = contrato()
                .andExpect(jsonPath("$.paths.keys()", hasSize(OPERACIONES.size())))
                .andExpect(jsonPath("$.paths.keys()",
                        containsInAnyOrder(OPERACIONES.keySet().toArray(String[]::new))));
        for (Map.Entry<String, String> operacion : OPERACIONES.entrySet()) {
            contrato.andExpect(jsonPath("$.paths['" + operacion.getKey() + "'].keys()",
                    containsInAnyOrder(operacion.getValue())));
        }
    }

    @Test
    void debeDeclararElEsquemaDeSeguridadJwtGlobal() throws Exception {
        contrato()
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    void todasLasOperacionesDebenDocumentar401Y403ConApiError() throws Exception {
        ResultActions contrato = contrato();
        for (Map.Entry<String, String> operacion : OPERACIONES.entrySet()) {
            String base = "$.paths['" + operacion.getKey() + "']." + operacion.getValue() + ".responses";
            contrato.andExpect(jsonPath(base + "['401']").exists())
                    .andExpect(jsonPath(base + "['403']").exists())
                    .andExpect(jsonPath(base + "['401'].content.*.schema['$ref']",
                            hasItem("#/components/schemas/ApiError")))
                    .andExpect(jsonPath(base + "['403'].content.*.schema['$ref']",
                            hasItem("#/components/schemas/ApiError")));
        }
    }
}
