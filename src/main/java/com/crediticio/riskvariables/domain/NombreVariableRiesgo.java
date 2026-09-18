package com.crediticio.riskvariables.domain;

public enum NombreVariableRiesgo {
    INGRESOS_MENSUALES(TipoVariableRiesgo.NUMERICO),
    NIVEL_ENDEUDAMIENTO(TipoVariableRiesgo.NUMERICO),
    NUMERO_MORAS(TipoVariableRiesgo.NUMERICO),
    HISTORIAL_CREDITICIO(TipoVariableRiesgo.CATEGORICO),
    ANTIGUEDAD_LABORAL(TipoVariableRiesgo.NUMERICO);

    private final TipoVariableRiesgo tipo;

    NombreVariableRiesgo(TipoVariableRiesgo tipo) {
        this.tipo = tipo;
    }

    public TipoVariableRiesgo getTipo() {
        return tipo;
    }
}
