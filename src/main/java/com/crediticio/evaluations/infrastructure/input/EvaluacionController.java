package com.crediticio.evaluations.infrastructure.input;

import com.crediticio.evaluations.application.dto.CalcularScoreRequest;
import com.crediticio.evaluations.application.dto.EvaluacionResponse;
import com.crediticio.evaluations.ports.input.CalcularScoreUseCase;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluacionController {

    private final CalcularScoreUseCase calcularScoreUseCase;

    public EvaluacionController(CalcularScoreUseCase calcularScoreUseCase) {
        this.calcularScoreUseCase = calcularScoreUseCase;
    }

    @Operation(summary = "Calcular el score crediticio de un solicitante",
            description = "Identifica al solicitante por idSolicitante o numeroDocumento (exactamente uno de los "
                    + "dos), evalúa las reglas de scoring ACTIVAS asociadas a variables de riesgo ACTIVA contra "
                    + "sus datos, y registra una nueva evaluación con el detalle de cada regla considerada.")
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
                    description = "No es posible determinar NIVEL_ENDEUDAMIENTO porque los ingresos mensuales del "
                            + "solicitante son cero",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EvaluacionResponse> calcular(@Valid @RequestBody CalcularScoreRequest request) {
        EvaluacionResponse response = calcularScoreUseCase.calcular(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
