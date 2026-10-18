package com.crediticio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.crediticio.config.JwtConfig.ROL_ADMINISTRADOR;
import static com.crediticio.config.JwtConfig.ROL_ANALISTA;

/**
 * Matriz de permisos de scoring-api (HU10 CA02–CA05, docs/security/matriz-permisos-scoring-api.md).
 * Cada petición se autentica con su JWT (sin sesión HTTP ni llamadas a auth-api); el rol proviene
 * solo del token. Toda ruta no listada se deniega. El CSRF se desactiva porque la autenticación
 * viaja en la cabecera Authorization, no en cookies.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] RUTAS_SWAGGER = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /** Swagger solo es público cuando springdoc está habilitado (perfil dev); si no, sus rutas se deniegan. */
    @Value("${springdoc.api-docs.enabled:false}")
    private boolean swaggerHabilitado;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RespuestaSeguridadJson respuestas = new RespuestaSeguridadJson();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitantes").hasRole(ROL_ANALISTA)
                        .requestMatchers(HttpMethod.GET, "/api/v1/solicitantes/documento/*")
                                .hasAnyRole(ROL_ADMINISTRADOR, ROL_ANALISTA)
                        .requestMatchers(HttpMethod.POST, "/api/v1/evaluaciones").hasRole(ROL_ANALISTA)
                        // HU10 CA04 (P-HU10-01): ambos roles consultan; ejecutar el scoring sigue siendo del analista
                        .requestMatchers(HttpMethod.GET, "/api/v1/evaluaciones/*")
                                .hasAnyRole(ROL_ADMINISTRADOR, ROL_ANALISTA)
                        .requestMatchers(HttpMethod.POST, "/api/v1/variables-riesgo").hasRole(ROL_ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/variables-riesgo/*/estado")
                                .hasRole(ROL_ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/api/v1/reglas-scoring").hasRole(ROL_ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/reglas-scoring/*").hasRole(ROL_ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/api/v1/politicas-clasificacion/vigente")
                                .hasAnyRole(ROL_ADMINISTRADOR, ROL_ANALISTA)
                        .requestMatchers(HttpMethod.POST, "/api/v1/politicas-clasificacion")
                                .hasRole(ROL_ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(RUTAS_SWAGGER).access((autenticacion, contexto) ->
                                new AuthorizationDecision(swaggerHabilitado))
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(recursos -> recursos
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(JwtConfig.convertidorAutenticacion()))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "Authorization", "X-Trace-Id"));
        configuration.setExposedHeaders(List.of("X-Trace-Id"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
