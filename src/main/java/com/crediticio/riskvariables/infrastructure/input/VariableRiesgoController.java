package com.crediticio.riskvariables.infrastructure.input;

import com.crediticio.riskvariables.application.dto.CrearVariableRiesgoRequest;
import com.crediticio.riskvariables.application.dto.VariableRiesgoResponse;
import com.crediticio.riskvariables.ports.input.CrearVariableRiesgoUseCase;
import com.crediticio.shared.response.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/v1/variables-riesgo")
public class VariableRiesgoController {

    private final CrearVariableRiesgoUseCase crearVariableRiesgoUseCase;

    public VariableRiesgoController(CrearVariableRiesgoUseCase crearVariableRiesgoUseCase) {
        this.crearVariableRiesgoUseCase = crearVariableRiesgoUseCase;
    }

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
}
