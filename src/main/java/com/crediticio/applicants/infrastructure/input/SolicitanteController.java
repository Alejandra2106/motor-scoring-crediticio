package com.crediticio.applicants.infrastructure.input;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteResponse;
import com.crediticio.applicants.ports.input.RegistrarSolicitanteUseCase;
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
@RequestMapping("/api/v1/solicitantes")
public class SolicitanteController {

    private final RegistrarSolicitanteUseCase registrarSolicitanteUseCase;

    public SolicitanteController(RegistrarSolicitanteUseCase registrarSolicitanteUseCase) {
        this.registrarSolicitanteUseCase = registrarSolicitanteUseCase;
    }

    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitante registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = SolicitanteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El número de documento ya se encuentra registrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SolicitanteResponse> registrar(@Valid @RequestBody RegistrarSolicitanteRequest request) {
        SolicitanteResponse response = registrarSolicitanteUseCase.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
