package com.crediticio.evaluations.application;

import com.crediticio.evaluations.domain.PoliticaReintentos;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort;
import com.crediticio.evaluations.ports.output.GeneracionRecomendacionesPort.ModoIntento;
import com.crediticio.evaluations.ports.output.ResultadoGeneracion;
import com.crediticio.evaluations.ports.output.TrabajoRecomendacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Un intento de la saga de recomendaciones (HU14): pide la generación a evaluation-query-api fuera de toda
 * transacción y registra el resultado con el token de la concesión vigente. La evaluación solo se completa
 * con una generación confirmada (CA13). Un éxito remoto cuyo registro falla se recupera en el siguiente
 * intento, que recibe la generación ya guardada (CA15).
 */
@Service
public class ProcesoGeneracionRecomendaciones {

    private static final Logger log = LoggerFactory.getLogger(ProcesoGeneracionRecomendaciones.class);
    static final String REINTENTOS_AGOTADOS = "REINTENTOS_AGOTADOS";

    private final GeneracionRecomendacionesPort generacionPort;
    private final TrabajoRecomendacionPort trabajoPort;
    private final PoliticaReintentos politica;

    public ProcesoGeneracionRecomendaciones(GeneracionRecomendacionesPort generacionPort,
            TrabajoRecomendacionPort trabajoPort, PoliticaReintentos politica) {
        this.generacionPort = generacionPort;
        this.trabajoPort = trabajoPort;
        this.politica = politica;
    }

    /**
     * @param intentos número de este intento dentro del ciclo (1 = intento en línea)
     * @return true solo si la evaluación quedó COMPLETADA con este intento
     */
    public boolean intentar(long idEvaluacion, UUID token, ModoIntento modo, int intentos) {
        ResultadoGeneracion resultado = generacionPort.generar(idEvaluacion, modo);
        return switch (resultado) {
            case ResultadoGeneracion.Exito exito -> registrarExito(idEvaluacion, token, exito);
            case ResultadoGeneracion.ErrorTransitorio error -> {
                registrarErrorTransitorio(idEvaluacion, token, intentos, error);
                yield false;
            }
            case ResultadoGeneracion.ErrorPermanente error -> {
                log.error("Saga de recomendaciones: error permanente en la evaluación {} ({}); requiere intervención",
                        idEvaluacion, error.codigo());
                trabajoPort.registrarErrorPermanente(idEvaluacion, token, error.codigo(), error.detalle());
                yield false;
            }
            case ResultadoGeneracion.NoDisponible noDisponible -> false;
        };
    }

    private boolean registrarExito(long idEvaluacion, UUID token, ResultadoGeneracion.Exito exito) {
        boolean registrado = trabajoPort.registrarExito(idEvaluacion, token);
        if (registrado) {
            log.info("Evaluación {} COMPLETADA con {} recomendaciones (catálogo {})", idEvaluacion, exito.cantidad(),
                    exito.versionCatalogo());
        } else {
            log.warn("Evaluación {}: la concesión ya no es la vigente; el resultado se descarta", idEvaluacion);
        }
        return registrado;
    }

    private void registrarErrorTransitorio(long idEvaluacion, UUID token, int intentos,
            ResultadoGeneracion.ErrorTransitorio error) {
        if (politica.admiteOtroIntento(intentos)) {
            log.warn("Saga de recomendaciones: error transitorio en la evaluación {} ({}), intento {}", idEvaluacion,
                    error.codigo(), intentos);
            trabajoPort.registrarErrorTransitorio(idEvaluacion, token, politica.esperaTras(intentos), error.codigo(),
                    error.detalle());
        } else {
            log.error("Saga de recomendaciones: la evaluación {} agotó sus {} intentos ({}); requiere intervención",
                    idEvaluacion, intentos, error.codigo());
            trabajoPort.registrarErrorPermanente(idEvaluacion, token, REINTENTOS_AGOTADOS,
                    error.codigo() + ": " + error.detalle());
        }
    }
}
