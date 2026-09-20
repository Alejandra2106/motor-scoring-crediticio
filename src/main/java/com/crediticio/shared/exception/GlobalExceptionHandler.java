package com.crediticio.shared.exception;

import com.crediticio.applicants.domain.DocumentoDuplicadoException;
import com.crediticio.applicants.domain.SolicitanteNoEncontradoException;
import com.crediticio.riskvariables.domain.VariableRiesgoDuplicadaException;
import com.crediticio.riskvariables.domain.VariableRiesgoNoEncontradaException;
import com.crediticio.scoring.domain.ReglaScoringDuplicadaException;
import com.crediticio.scoring.domain.ReglaScoringVariableInactivaException;
import com.crediticio.scoring.domain.ReglaScoringVariableNoEncontradaException;
import com.crediticio.shared.response.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(SolicitanteNoEncontradoException.class)
    public ResponseEntity<ApiError> manejarSolicitanteNoEncontrado(SolicitanteNoEncontradoException ex) {
        log.warn("Consulta de un solicitante que no existe");
        return construirRespuesta(HttpStatus.NOT_FOUND, "SOLICITANTE_NO_ENCONTRADO", ex.getMessage(), List.of());
    }

    @ExceptionHandler(VariableRiesgoDuplicadaException.class)
    public ResponseEntity<ApiError> manejarVariableRiesgoDuplicada(VariableRiesgoDuplicadaException ex) {
        log.warn("Intento de creación de una variable de riesgo con un nombre ya existente");
        return construirRespuesta(HttpStatus.CONFLICT, "VARIABLE_RIESGO_DUPLICADA", ex.getMessage(), List.of());
    }

    @ExceptionHandler(VariableRiesgoNoEncontradaException.class)
    public ResponseEntity<ApiError> manejarVariableRiesgoNoEncontrada(VariableRiesgoNoEncontradaException ex) {
        log.warn("Operación sobre una variable de riesgo que no existe");
        return construirRespuesta(HttpStatus.NOT_FOUND, "VARIABLE_RIESGO_NO_ENCONTRADA", ex.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaScoringVariableNoEncontradaException.class)
    public ResponseEntity<ApiError> manejarReglaScoringVariableNoEncontrada(ReglaScoringVariableNoEncontradaException ex) {
        log.warn("Intento de creación de una regla de scoring sobre una variable de riesgo inexistente");
        return construirRespuesta(HttpStatus.NOT_FOUND, "VARIABLE_RIESGO_NO_ENCONTRADA", ex.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaScoringVariableInactivaException.class)
    public ResponseEntity<ApiError> manejarReglaScoringVariableInactiva(ReglaScoringVariableInactivaException ex) {
        log.warn("Intento de creación de una regla de scoring sobre una variable de riesgo inactiva");
        return construirRespuesta(HttpStatus.CONFLICT, "VARIABLE_RIESGO_INACTIVA", ex.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaScoringDuplicadaException.class)
    public ResponseEntity<ApiError> manejarReglaScoringDuplicada(ReglaScoringDuplicadaException ex) {
        log.warn("Intento de creación de una regla de scoring duplicada");
        return construirRespuesta(HttpStatus.CONFLICT, "REGLA_SCORING_DUPLICADA", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> manejarTipoDeArgumentoInvalido(MethodArgumentTypeMismatchException ex) {
        log.warn("Solicitud rechazada por un parámetro con formato inválido");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "El parámetro '" + ex.getName() + "' tiene un formato inválido", List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> manejarViolacionDeRestriccion(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        log.warn("Solicitud rechazada por violación de una restricción de validación");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Los datos enviados no son válidos", details);
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
