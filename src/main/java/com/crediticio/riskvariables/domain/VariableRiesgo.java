package com.crediticio.riskvariables.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class VariableRiesgo {

    private final Long idVariableRiesgo;
    private final NombreVariableRiesgo variable;
    private final String descripcion;
    private final EstadoVariableRiesgo estado;
    private final LocalDateTime fechaCreacion;

    private VariableRiesgo(Long idVariableRiesgo, NombreVariableRiesgo variable, String descripcion,
            EstadoVariableRiesgo estado, LocalDateTime fechaCreacion) {
        this.idVariableRiesgo = idVariableRiesgo;
        this.variable = Objects.requireNonNull(variable, "variable es obligatoria");
        this.descripcion = validarDescripcion(descripcion);
        this.estado = Objects.requireNonNull(estado, "estado es obligatorio");
        this.fechaCreacion = fechaCreacion;
    }

    public static VariableRiesgo nueva(NombreVariableRiesgo variable, String descripcion) {
        return new VariableRiesgo(null, variable, descripcion, EstadoVariableRiesgo.ACTIVA, null);
    }

    public static VariableRiesgo reconstruir(Long idVariableRiesgo, NombreVariableRiesgo variable, String descripcion,
            EstadoVariableRiesgo estado, LocalDateTime fechaCreacion) {
        return new VariableRiesgo(idVariableRiesgo, variable, descripcion, estado, fechaCreacion);
    }

    private static String validarDescripcion(String descripcion) {
        if (descripcion == null) {
            throw new IllegalArgumentException("descripcion es obligatoria");
        }
        String descripcionNormalizada = descripcion.trim();
        if (descripcionNormalizada.length() < 10 || descripcionNormalizada.length() > 255) {
            throw new IllegalArgumentException("descripcion debe tener entre 10 y 255 caracteres");
        }
        return descripcionNormalizada;
    }

    public Long getIdVariableRiesgo() {
        return idVariableRiesgo;
    }

    public NombreVariableRiesgo getVariable() {
        return variable;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoVariableRiesgo getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public TipoVariableRiesgo getTipo() {
        return variable.getTipo();
    }
}
