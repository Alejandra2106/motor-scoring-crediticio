package com.crediticio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI de scoring-api (HU10): todos los endpoints de negocio exigen un JWT de acceso
 * emitido por auth-api (esquema bearerAuth) y pueden responder 401/403 con el formato ApiError.
 * Las respuestas de seguridad se documentan aquí una sola vez en lugar de repetirse en cada
 * controlador; un controlador puede seguir documentándolas con un texto propio.
 */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_SEGURIDAD = "bearerAuth";
    private static final String REFERENCIA_API_ERROR = "#/components/schemas/ApiError";

    @Bean
    public OpenAPI scoringApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("scoring-api - Motor de Scoring de Riesgo Crediticio")
                        .version("v1")
                        .description("Solicitantes, variables de riesgo, reglas de scoring, cálculo del score "
                                + "y políticas de clasificación. Requiere un JWT de acceso emitido por auth-api."))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de acceso RS256 emitido por auth-api")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
    }

    @Bean
    public OpenApiCustomizer respuestasDeSeguridad() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(operacion -> {
                if (operacion.getResponses() == null) {
                    operacion.setResponses(new ApiResponses());
                }
                ApiResponses respuestas = operacion.getResponses();
                if (!respuestas.containsKey("401")) {
                    respuestas.addApiResponse("401",
                            respuestaError("Token de acceso ausente, inválido o expirado"));
                }
                if (!respuestas.containsKey("403")) {
                    respuestas.addApiResponse("403",
                            respuestaError("El usuario no tiene un rol autorizado para esta operación"));
                }
            }));
        };
    }

    private static ApiResponse respuestaError(String descripcion) {
        return new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(REFERENCIA_API_ERROR))));
    }
}
