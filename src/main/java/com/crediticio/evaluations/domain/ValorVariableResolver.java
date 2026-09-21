package com.crediticio.evaluations.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Resuelve el valor real del solicitante correspondiente a una variable de riesgo activa,
 * como texto comparable contra {@code valorCondicion} de una regla de scoring (RF06).
 *
 * RF05 (validación defensiva): los campos financieros del solicitante son obligatorios desde
 * HU01 (restricciones {@code NOT NULL} en la tabla {@code solicitante}), por lo que un dato
 * nulo no debería poder llegar aquí; se valida de todas formas para no asumir silenciosamente
 * un valor incorrecto si esa garantía llegara a fallar.
 */
public final class ValorVariableResolver {

    private ValorVariableResolver() {
    }

    public static String resolver(NombreVariableEvaluada variable, SolicitanteParaEvaluacion solicitante) {
        return switch (variable) {
            case INGRESOS_MENSUALES -> requerido(solicitante.ingresosMensuales(), "ingresosMensuales").toPlainString();
            case NUMERO_MORAS -> String.valueOf(requerido(solicitante.numeroMoras(), "numeroMoras"));
            case HISTORIAL_CREDITICIO -> requerido(solicitante.historialCrediticio(), "historialCrediticio");
            case ANTIGUEDAD_LABORAL -> requerido(solicitante.antiguedadLaboral(), "antiguedadLaboral").toPlainString();
            case NIVEL_ENDEUDAMIENTO -> calcularNivelEndeudamiento(solicitante);
        };
    }

    private static String calcularNivelEndeudamiento(SolicitanteParaEvaluacion solicitante) {
        BigDecimal ingresosMensuales = requerido(solicitante.ingresosMensuales(), "ingresosMensuales");
        BigDecimal deudasMensuales = requerido(solicitante.deudasMensuales(), "deudasMensuales");
        if (ingresosMensuales.compareTo(BigDecimal.ZERO) == 0) {
            throw new NivelEndeudamientoIndeterminadoException();
        }
        return deudasMensuales
                .divide(ingresosMensuales, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .toPlainString();
    }

    private static <T> T requerido(T valor, String nombreCampo) {
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Falta el dato requerido del solicitante para calcular el score: " + nombreCampo);
        }
        return valor;
    }
}
