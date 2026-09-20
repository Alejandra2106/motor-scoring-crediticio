package com.crediticio.riskvariables.infrastructure.input;

import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.CambiarEstadoVariableRiesgoResponse;
import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;
import com.crediticio.riskvariables.ports.input.CambiarEstadoVariableRiesgoUseCase;
import com.crediticio.riskvariables.ports.input.CrearVariableRiesgoUseCase;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/variables-riesgo")
public class VariableRiesgoController {

    private final CrearVariableRiesgoUseCase crearVariableRiesgoUseCase;
    private final CambiarEstadoVariableRiesgoUseCase cambiarEstadoVariableRiesgoUseCase;

    public VariableRiesgoController(CrearVariableRiesgoUseCase crearVariableRiesgoUseCase,
            CambiarEstadoVariableRiesgoUseCase cambiarEstadoVariableRiesgoUseCase) {
        this.crearVariableRiesgoUseCase = crearVariableRiesgoUseCase;
        this.cambiarEstadoVariableRiesgoUseCase = cambiarEstadoVariableRiesgoUseCase;
    }

    @Operation(summary = "Registrar una variable de riesgo",
            description = "Crea una nueva variable de riesgo a partir del catálogo cerrado de variables soportado por el sistema, junto con su descripción.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = CrearVariableRiesgoRequest.class),
                    examples = @ExampleObject(
                            name = "Solicitud válida",
                            value = "{\n  \"variable\": \"INGRESOS_MENSUALES\",\n  \"descripcion\": \"Ingresos mensuales del solicitante\"\n}")))
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Variable de riesgo creada exitosamente",
                    content = @Content(schema = @Schema(implementation = VariableRiesgoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "La variable de riesgo ya se encuentra registrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<VariableRiesgoResponse> crear(@Valid @RequestBody CrearVariableRiesgoRequest request) {
        VariableRiesgoResponse response = crearVariableRiesgoUseCase.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{idRiesgo}/estado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la variable de riesgo actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = CambiarEstadoVariableRiesgoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido, ausente o idRiesgo con formato inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No se encontró la variable de riesgo",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CambiarEstadoVariableRiesgoResponse> cambiarEstado(
            @PathVariable Long idRiesgo,
            @Valid @RequestBody CambiarEstadoVariableRiesgoRequest request) {
        CambiarEstadoVariableRiesgoResponse response = cambiarEstadoVariableRiesgoUseCase.cambiarEstado(idRiesgo, request);
        return ResponseEntity.ok(response);
    }
}
