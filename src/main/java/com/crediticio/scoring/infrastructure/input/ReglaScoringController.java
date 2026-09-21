package com.crediticio.scoring.infrastructure.input;

import com.crediticio.scoring.application.dto.CrearReglaScoringRequest;
import com.crediticio.scoring.application.dto.ReglaScoringResponse;
import com.crediticio.scoring.ports.input.CrearReglaScoringUseCase;
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
@RequestMapping("/api/v1/reglas-scoring")
public class ReglaScoringController {

    private final CrearReglaScoringUseCase crearReglaScoringUseCase;

    public ReglaScoringController(CrearReglaScoringUseCase crearReglaScoringUseCase) {
        this.crearReglaScoringUseCase = crearReglaScoringUseCase;
    }

    @Operation(summary = "Crear una regla de scoring",
            description = "Crea una nueva regla de scoring asociada a una variable de riesgo activa, "
                    + "estableciendo una condición (operador y valor) y el puntaje que aporta al cálculo del score.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = CrearReglaScoringRequest.class),
                    examples = {
                            @ExampleObject(name = "Condición numérica",
                                    value = "{\n  \"idRiesgo\": 1,\n  \"operador\": \">=\",\n  \"valorCondicion\": \"3000000\",\n  \"puntaje\": 20\n}"),
                            @ExampleObject(name = "Condición categórica",
                                    value = "{\n  \"idRiesgo\": 4,\n  \"operador\": \"=\",\n  \"valorCondicion\": \"BUENO\",\n  \"puntaje\": 30\n}")
                    }))
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Regla de scoring creada exitosamente",
                    content = @Content(schema = @Schema(implementation = ReglaScoringResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "La variable de riesgo asociada no existe",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409",
                    description = "La variable de riesgo está inactiva, o ya existe una regla con la misma condición",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReglaScoringResponse> crear(@Valid @RequestBody CrearReglaScoringRequest request) {
        ReglaScoringResponse response = crearReglaScoringUseCase.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
