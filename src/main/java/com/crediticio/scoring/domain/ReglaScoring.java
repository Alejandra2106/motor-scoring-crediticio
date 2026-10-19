package com.crediticio.scoring.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public class ReglaScoring {

    private static final int PUNTAJE_MINIMO = -100;
    private static final int PUNTAJE_MAXIMO = 100;
    private static final Set<String> VALORES_HISTORIAL_CREDITICIO = Set.of("BUENO", "REGULAR", "MALO");

    private final Long idRegla;
    private final Long idRiesgo;
    private final OperadorScoring operador;
    private final String valorCondicion;
    private final Integer puntaje;
    private final EstadoReglaScoring estado;
    private final LocalDateTime fechaCreacion;

    private ReglaScoring(Long idRegla, Long idRiesgo, OperadorScoring operador, String valorCondicion,
            Integer puntaje, EstadoReglaScoring estado, LocalDateTime fechaCreacion) {
        this.idRegla = idRegla;
        this.idRiesgo = Objects.requireNonNull(idRiesgo, "idRiesgo es obligatorio");
        this.operador = Objects.requireNonNull(operador, "operador es obligatorio");
        this.valorCondicion = validarValorCondicion(valorCondicion);
        this.puntaje = validarPuntaje(puntaje);
        this.estado = Objects.requireNonNull(estado, "estado es obligatorio");
        this.fechaCreacion = fechaCreacion;
    }

    public static ReglaScoring nueva(Long idRiesgo, OperadorScoring operador, String valorCondicion, Integer puntaje,
            TipoVariable tipoVariable, boolean permiteDecimales) {
        validarCompatibilidadOperadorTipo(operador, tipoVariable);
        ReglaScoring regla = new ReglaScoring(null, idRiesgo, operador, valorCondicion, puntaje,
                EstadoReglaScoring.ACTIVA, null);
        validarValorSegunTipo(regla.valorCondicion, tipoVariable, permiteDecimales);
        return regla;
    }

    public static ReglaScoring reconstruir(Long idRegla, Long idRiesgo, OperadorScoring operador,
            String valorCondicion, Integer puntaje, EstadoReglaScoring estado, LocalDateTime fechaCreacion) {
        return new ReglaScoring(idRegla, idRiesgo, operador, valorCondicion, puntaje, estado, fechaCreacion);
    }

    public ReglaScoring editar(OperadorScoring operador, String valorCondicion, Integer puntaje,
            TipoVariable tipoVariable, boolean permiteDecimales) {
        validarCompatibilidadOperadorTipo(operador, tipoVariable);
        ReglaScoring editada = new ReglaScoring(idRegla, idRiesgo, operador, valorCondicion, puntaje, estado,
                fechaCreacion);
        validarValorSegunTipo(editada.valorCondicion, tipoVariable, permiteDecimales);
        return editada;
    }

    private static void validarCompatibilidadOperadorTipo(OperadorScoring operador, TipoVariable tipoVariable) {
        Objects.requireNonNull(tipoVariable, "tipoVariable es obligatorio");
        if (tipoVariable == TipoVariable.CATEGORICO && !operador.esSoloIgualdad()) {
            throw new IllegalArgumentException("Las variables categóricas únicamente admiten el operador '='");
        }
    }

    private static void validarValorSegunTipo(String valorCondicion, TipoVariable tipoVariable,
            boolean permiteDecimales) {
        if (tipoVariable == TipoVariable.NUMERICO) {
            validarValorNumerico(valorCondicion, permiteDecimales);
        } else {
            validarValorCategorico(valorCondicion);
        }
    }

    private static String validarValorCondicion(String valorCondicion) {
        if (valorCondicion == null || valorCondicion.isBlank()) {
            throw new IllegalArgumentException("valorCondicion es obligatorio");
        }
        return valorCondicion.trim();
    }

    private static void validarValorNumerico(String valorCondicion, boolean permiteDecimales) {
        BigDecimal valor;
        try {
            valor = new BigDecimal(valorCondicion);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("valorCondicion debe ser un valor numérico válido");
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("valorCondicion debe ser mayor o igual a 0");
        }
        if (!permiteDecimales && valor.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("valorCondicion debe ser un número entero para esta variable");
        }
    }

    private static void validarValorCategorico(String valorCondicion) {
        if (!VALORES_HISTORIAL_CREDITICIO.contains(valorCondicion)) {
            throw new IllegalArgumentException("valorCondicion debe ser BUENO, REGULAR o MALO");
        }
    }

    private static Integer validarPuntaje(Integer puntaje) {
        if (puntaje == null) {
            throw new IllegalArgumentException("puntaje es obligatorio");
        }
        if (puntaje < PUNTAJE_MINIMO || puntaje > PUNTAJE_MAXIMO) {
            throw new IllegalArgumentException("puntaje debe estar entre -100 y 100");
        }
        return puntaje;
    }

    public Long getIdRegla() {
        return idRegla;
    }

    public Long getIdRiesgo() {
        return idRiesgo;
    }

    public OperadorScoring getOperador() {
        return operador;
    }

    public String getValorCondicion() {
        return valorCondicion;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public EstadoReglaScoring getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Determina si esta regla se cumple para un valor real del solicitante, reutilizando
     * la semántica de {@link OperadorScoring} ya validada en la creación/edición de la regla.
     * Para variables categóricas el único operador permitido es la igualdad exacta de texto.
     */
    public boolean evaluar(String valorReal, TipoVariable tipoVariable) {
        Objects.requireNonNull(valorReal, "valorReal es obligatorio");
        Objects.requireNonNull(tipoVariable, "tipoVariable es obligatorio");
        if (tipoVariable == TipoVariable.NUMERICO) {
            BigDecimal real = new BigDecimal(valorReal);
            BigDecimal condicion = new BigDecimal(valorCondicion);
            return operador.comparar(real.compareTo(condicion));
        }
        return valorCondicion.equals(valorReal);
    }
}
