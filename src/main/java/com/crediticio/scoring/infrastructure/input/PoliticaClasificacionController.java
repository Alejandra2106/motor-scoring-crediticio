package com.crediticio.scoring.infrastructure.input;

import com.crediticio.scoring.application.dto.PoliticaClasificacionResponse;
import com.crediticio.scoring.application.dto.PublicarPoliticaClasificacionRequest;
import com.crediticio.scoring.ports.input.ConsultarPoliticaVigenteUseCase;
import com.crediticio.scoring.ports.input.PublicarPoliticaClasificacionUseCase;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU11: consulta de la política de clasificación vigente (ambos roles) y publicación de una nueva
 * versión (solo ADMINISTRADOR_RIESGO, según la matriz de HU10 en SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/politicas-clasificacion")
public class PoliticaClasificacionController {

    private final ConsultarPoliticaVigenteUseCase consultarPoliticaVigenteUseCase;
    private final PublicarPoliticaClasificacionUseCase publicarPoliticaClasificacionUseCase;

    public PoliticaClasificacionController(ConsultarPoliticaVigenteUseCase consultarPoliticaVigenteUseCase,
            PublicarPoliticaClasificacionUseCase publicarPoliticaClasificacionUseCase) {
        this.consultarPoliticaVigenteUseCase = consultarPoliticaVigenteUseCase;
        this.publicarPoliticaClasificacionUseCase = publicarPoliticaClasificacionUseCase;
    }

    @Operation(summary = "Consultar la política de clasificación vigente",
            description = "Retorna la versión vigente de la política de clasificación con sus rangos de puntaje "
                    + "para las categorías BAJO, MEDIO y ALTO.")
    @GetMapping("/vigente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Política de clasificación vigente",
                    content = @Content(schema = @Schema(implementation = PoliticaClasificacionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token de acceso ausente, inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene un rol autorizado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe una política de clasificación vigente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PoliticaClasificacionResponse> consultarVigente() {
        return ResponseEntity.ok(consultarPoliticaVigenteUseCase.consultarVigente());
    }

    @Operation(summary = "Publicar una nueva versión de la política de clasificación",
            description = "Valida en conjunto los rangos (una categoría por nivel, límites enteros entre -100 y 130, "
                    + "sin solapamientos ni huecos y cobertura completa) y, si son válidos, publica una nueva versión "
                    + "que pasa a ser la vigente. Las versiones anteriores se conservan sin cambios.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = PublicarPoliticaClasificacionRequest.class),
                    examples = @ExampleObject(name = "Política completa",
                            value = "{\n  \"rangos\": [\n"
                                    + "    {\"categoria\": \"ALTO\", \"limiteInferior\": -100, \"limiteSuperior\": 29},\n"
                                    + "    {\"categoria\": \"MEDIO\", \"limiteInferior\": 30, \"limiteSuperior\": 79},\n"
                                    + "    {\"categoria\": \"BAJO\", \"limiteInferior\": 80, \"limiteSuperior\": 130}\n"
                                    + "  ]\n}")))
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nueva versión publicada y vigente",
                    content = @Content(schema = @Schema(implementation = PoliticaClasificacionResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Datos inválidos o política que no supera las validaciones (todos los errores en details)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token de acceso ausente, inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ADMINISTRADOR_RIESGO",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Otra publicación concurrente obtuvo la misma versión",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno no controlado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PoliticaClasificacionResponse> publicar(
            @Valid @RequestBody PublicarPoliticaClasificacionRequest request) {
        PoliticaClasificacionResponse response = publicarPoliticaClasificacionUseCase.publicar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
