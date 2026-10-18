-- HU11 CA11: las versiones publicadas de la política de clasificación y sus rangos son inmutables.
-- Hasta ahora lo garantizaba solo la aplicación (el puerto no ofrece modificación ni eliminación y las
-- entidades JPA son updatable = false). Esta migración lo garantiza también en la base de datos
-- (decisión P2, opción C, 2026-10-17).
--
-- Publicar una versión nueva solo inserta filas en politica_clasificacion y rango_clasificacion, y
-- actualiza el puntero de configuracion_clasificacion_actual, que NO se protege aquí. No hace falta
-- bloquear los INSERT de rangos: UNIQUE (id_politica, categoria) y el CHECK de las tres categorías
-- (V6) ya impiden añadir un cuarto rango a una política completa.
--
-- Se bloquean UPDATE y DELETE (triggers por fila) y TRUNCATE (trigger por sentencia, porque los
-- triggers por fila no se disparan con TRUNCATE). Las versiones históricas sin autor (V7) quedan
-- también inmutables: no se completa su autor.
--
-- Corrección excepcional: solo el propietario de la tabla puede desactivar estos triggers
-- (ALTER TABLE ... DISABLE TRIGGER ...), en un procedimiento administrativo explícito.

CREATE FUNCTION fn_tg_politica_clasificacion_inmutable() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Las versiones publicadas de la política de clasificación y sus rangos son inmutables (% sobre %)',
        TG_OP, TG_TABLE_NAME
        USING ERRCODE = 'object_not_in_prerequisite_state';
END;
$$;

CREATE TRIGGER tg_politica_clasificacion_inmutable
    BEFORE UPDATE OR DELETE ON politica_clasificacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_politica_clasificacion_inmutable();

CREATE TRIGGER tg_politica_clasificacion_sin_truncate
    BEFORE TRUNCATE ON politica_clasificacion
    FOR EACH STATEMENT EXECUTE FUNCTION fn_tg_politica_clasificacion_inmutable();

CREATE TRIGGER tg_rango_clasificacion_inmutable
    BEFORE UPDATE OR DELETE ON rango_clasificacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_politica_clasificacion_inmutable();

CREATE TRIGGER tg_rango_clasificacion_sin_truncate
    BEFORE TRUNCATE ON rango_clasificacion
    FOR EACH STATEMENT EXECUTE FUNCTION fn_tg_politica_clasificacion_inmutable();
