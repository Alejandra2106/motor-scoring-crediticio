package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
import com.crediticio.evaluations.ports.input.ConsultarEvaluacionUseCase;
import com.crediticio.shared.response.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluacionController {

    private final CalcularScoreUseCase calcularScoreUseCase;
    private final ConsultarEvaluacionUseCase consultarEvaluacionUseCase;

    public EvaluacionController(CalcularScoreUseCase calcularScoreUseCase,
            ConsultarEvaluacionUseCase consultarEvaluacionUseCase) {
        this.calcularScoreUseCase = calcularScoreUseCase;
        this.consultarEvaluacionUseCase = consultarEvaluacionUseCase;
    }

    @Operation(summary = "Calcular el score crediticio de un solicitante",
            description = "Identifica al solicitante por idSolicitante o numeroDocumento (exactamente uno de los "
                    + "dos), evalúa las reglas de scoring ACTIVAS asociadas a variables de riesgo ACTIVA contra "
                    + "sus datos, clasifica el puntaje total con la política de clasificación vigente y registra "
                    + "una nueva evaluación con su categoría, la versión de política utilizada y el detalle de "
                    + "cada regla considerada. La categoría describe el riesgo; no es una aprobación ni un "
                    + "rechazo del crédito.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = CalcularScoreRequest.class),
                    examples = {
                            @ExampleObject(name = "Identificación por idSolicitante", value = "{\n  \"idSolicitante\": 1\n}"),
                            @ExampleObject(name = "Identificación por numeroDocumento",
                                    value = "{\n  \"numeroDocumento\": \"1234567890\"\n}")
                    }))
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evaluación calculada y registrada exitosamente",
                    content = @Content(schema = @Schema(implementation = EvaluacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos, o falta información "
                    + "del solicitante necesaria para el cálculo",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "El solicitante indicado no existe",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409",
                    description = "No se registró la evaluación: NIVEL_ENDEUDAMIENTO es indeterminado porque los "
                            + "ingresos mensuales del solicitante son cero (NIVEL_ENDEUDAMIENTO_INDETERMINADO), no "
                            + "existe una política de clasificación vigente (POLITICA_VIGENTE_NO_DISPONIBLE) o el "
                            + "puntaje total no pertenece a ningún rango de la política (PUNTAJE_SIN_CLASIFICACION)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EvaluacionResponse> calcular(@Valid @RequestBody CalcularScoreRequest request) {
        EvaluacionResponse response = calcularScoreUseCase.calcular(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar una evaluación por su identificador",
            description = "Retorna el puntaje total, la categoría de riesgo, la versión de la política de "
                    + "clasificación utilizada y el detalle de la evaluación, tal como se registraron. Las "
                    + "evaluaciones registradas antes de la clasificación tienen categoría y política nulas.")
    @GetMapping("/{idEvaluacion}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación encontrada",
                    content = @Content(schema = @Schema(implementation = EvaluacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "El identificador tiene un formato inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "La evaluación indicada no existe",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EvaluacionResponse> consultarPorId(@PathVariable Long idEvaluacion) {
        return ResponseEntity.ok(consultarEvaluacionUseCase.consultarPorId(idEvaluacion));
    }
}
