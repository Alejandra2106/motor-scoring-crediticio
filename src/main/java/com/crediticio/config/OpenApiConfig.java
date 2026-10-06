package com.crediticio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;






@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI motorScoringOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Motor de Scoring de Riesgo Crediticio")
                        .description("API para la gestión de solicitantes, variables de riesgo, "
                                + "reglas de scoring y evaluaciones crediticias.")
                        .version("1.0.0"));
    }
}
         