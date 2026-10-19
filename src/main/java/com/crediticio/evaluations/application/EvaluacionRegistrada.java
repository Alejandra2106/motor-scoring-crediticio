package com.crediticio.evaluations.application;

import com.crediticio.evaluations.domain.Evaluacion;

import java.util.UUID;

/** Evaluación recién confirmada (PENDIENTE) y el token de la concesión de su intento en línea (HU14). */
public record EvaluacionRegistrada(Evaluacion evaluacion, UUID tokenConcesion) {
}
