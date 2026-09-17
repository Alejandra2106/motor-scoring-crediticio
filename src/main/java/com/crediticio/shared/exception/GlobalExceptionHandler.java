package com.crediticio.shared.exception;

import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.shared.response.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        log.warn("Solicitud rechazada por datos inválidos");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Los datos enviados no son válidos", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        log.warn("Solicitud rechazada por cuerpo no legible o con formato inválido");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "El cuerpo de la solicitud no es válido", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> manejarArgumentoInvalido(IllegalArgumentException ex) {
        log.warn("Solicitud rechazada por violación de una invariante de negocio");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    public ResponseEntity<ApiError> manejarDocumentoDuplicado(DocumentoDuplicadoException ex) {
        log.warn("Intento de registro con un número de documento ya existente");
        return construirRespuesta(HttpStatus.CONFLICT, "DOCUMENTO_DUPLICADO", ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorInterno(Exception ex) {
        log.error("Error interno no controlado", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno. Intente nuevamente más tarde", List.of());
    }

    private ResponseEntity<ApiError> construirRespuesta(HttpStatus status, String errorCode, String message,
            List<String> details) {
        ApiError apiError = new ApiError(errorCode, message, details, MDC.get("traceId"));
        return ResponseEntity.status(status).body(apiError);
    }
}
