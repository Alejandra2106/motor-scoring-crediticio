package com.crediticio.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 401 (sin token, token inválido o expirado) y 403 (rol insuficiente) con el formato ApiError
 * (HU10 CA06–CA07), igual que auth-api. Los mensajes son fijos y no revelan detalles del token.
 */
public class RespuestaSeguridadJson implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String MDC_TRACE_ID = "traceId";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO",
                "Se requiere un token de acceso válido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpServletResponse.SC_FORBIDDEN, "ACCESO_DENEGADO",
                "No tiene permisos para realizar esta operación");
    }

    private static void escribir(HttpServletResponse response, int estado, String codigo, String mensaje)
            throws IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // el traceId solo contiene [0-9a-f-] (UUID validado por TraceIdFilter), y los textos son constantes
        String traceId = MDC.get(MDC_TRACE_ID);
        response.getWriter().write("{\"errorCode\":\"" + codigo + "\",\"message\":\"" + mensaje
                + "\",\"details\":[],\"traceId\":" + (traceId == null ? "null" : "\"" + traceId + "\"") + "}");
    }
}
