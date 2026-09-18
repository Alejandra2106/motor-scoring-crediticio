package com.crediticio.applicants.infrastructure.input;

import com.crediticio.applicants.application.dto.RegistrarSolicitanteRequest;
import com.crediticio.applicants.application.dto.SolicitanteDetalleResponse;
import com.crediticio.applicants.application.dto.SolicitanteResponse;
import com.crediticio.applicants.ports.input.ConsultarSolicitanteUseCase;
import com.crediticio.applicants.ports.input.RegistrarSolicitanteUseCase;
import com.crediticio.shared.response.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solicitantes")
@Validated
public class SolicitanteController {

    private final RegistrarSolicitanteUseCase registrarSolicitanteUseCase;
    private final ConsultarSolicitanteUseCase consultarSolicitanteUseCase;

    public SolicitanteController(RegistrarSolicitanteUseCase registrarSolicitanteUseCase,
            ConsultarSolicitanteUseCase consultarSolicitanteUseCase) {
        this.registrarSolicitanteUseCase = registrarSolicitanteUseCase;
        this.consultarSolicitanteUseCase = consultarSolicitanteUseCase;
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

    @GetMapping("/documento/{numeroDocumento}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitante encontrado",
                    content = @Content(schema = @Schema(implementation = SolicitanteDetalleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Número de documento con formato inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No se encontró un solicitante con ese documento",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SolicitanteDetalleResponse> consultarPorDocumento(
            @PathVariable
            @Pattern(regexp = "\\d{6,15}", message = "numeroDocumento debe contener entre 6 y 15 dígitos")
            String numeroDocumento) {
        SolicitanteDetalleResponse response = consultarSolicitanteUseCase.consultarPorNumeroDocumento(numeroDocumento);
        return ResponseEntity.ok(response);
    }
}
