-- HU12 CA05: persistir en evaluacion el puntaje total, la categoría y la referencia a la
-- versión exacta de la política de clasificación usada. score_total es un dato derivable de
-- detalle_evaluacion; se persiste por excepción aprobada a CLAUDE.md §12, solo para HU12 CA05.
--
-- Evaluaciones históricas (anteriores a V8): quedan con score_total, categoria e id_politica en
-- NULL y no se reclasifican (decisión 7). Las restricciones nuevas que exigen esos datos se
-- crean NOT VALID para no comprobar esas filas; no ejecutar VALIDATE CONSTRAINT mientras existan.
--
-- Las funciones fijan search_path al esquema en el que se aplica la migración (placeholder
-- nativo de Flyway flyway:defaultSchema, usado en la cláusula SET de cada función: "public" en
-- ejecución normal, el esquema dedicado en pruebas), para no depender del search_path de cada sesión.

-- 1. Columnas (sin valor por defecto: las filas existentes quedan en NULL).
ALTER TABLE evaluacion
    ADD COLUMN score_total INTEGER,
    ADD COLUMN categoria   VARCHAR(10),
    ADD COLUMN id_politica BIGINT;

-- 2. Integridad referencial: la evaluación apunta a una versión publicada. Sin ON DELETE: una
--    política referenciada no puede eliminarse (refuerza HU11 CA11 y HU12 CA06).
ALTER TABLE evaluacion
    ADD CONSTRAINT fk_evaluacion_politica
        FOREIGN KEY (id_politica) REFERENCES politica_clasificacion (id_politica);

-- 3. Dominio de la categoría (mismos valores que rango_clasificacion.categoria). Admite NULL.
ALTER TABLE evaluacion
    ADD CONSTRAINT ck_evaluacion_categoria
        CHECK (categoria IN ('BAJO', 'MEDIO', 'ALTO'));

-- 4. Toda evaluación nueva debe estar clasificada (CA03/CA05). NOT VALID: no se comprueban las
--    evaluaciones históricas, pero sí toda fila insertada o actualizada desde ahora.
ALTER TABLE evaluacion
    ADD CONSTRAINT ck_evaluacion_clasificacion_completa
        CHECK (score_total IS NOT NULL AND categoria IS NOT NULL AND id_politica IS NOT NULL) NOT VALID;

CREATE INDEX ix_evaluacion_politica ON evaluacion (id_politica);

-- =====================================================================================
-- Trigger A (CA07/CA08): score_total = suma de detalle_evaluacion.puntaje_obtenido.
-- Constraint triggers diferidos: se comprueban al confirmar la transacción, cuando la evaluación
-- y todos sus detalles ya se insertaron. Si no coincide, la transacción completa se revierte.
-- Las evaluaciones históricas (score_total NULL) no se comprueban.
-- =====================================================================================
CREATE FUNCTION fn_verificar_score_total_evaluacion(p_id_evaluacion BIGINT) RETURNS VOID
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
DECLARE
    v_score_total INTEGER;
    v_suma        BIGINT;
BEGIN
    SELECT score_total INTO v_score_total FROM evaluacion WHERE id_evaluacion = p_id_evaluacion;
    IF NOT FOUND OR v_score_total IS NULL THEN
        RETURN;
    END IF;

    SELECT COALESCE(SUM(puntaje_obtenido), 0) INTO v_suma
    FROM detalle_evaluacion
    WHERE id_evaluacion = p_id_evaluacion;

    IF v_suma <> v_score_total THEN
        RAISE EXCEPTION 'score_total (%) de la evaluacion % no coincide con la suma de sus detalles (%)',
            v_score_total, p_id_evaluacion, v_suma
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ck_evaluacion_score_total_consistente';
    END IF;
END;
$$;

CREATE FUNCTION fn_tg_evaluacion_score_total() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    PERFORM fn_verificar_score_total_evaluacion(NEW.id_evaluacion);
    RETURN NULL;
END;
$$;

CREATE FUNCTION fn_tg_detalle_evaluacion_score_total() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM fn_verificar_score_total_evaluacion(OLD.id_evaluacion);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE')
            AND (TG_OP = 'INSERT' OR NEW.id_evaluacion IS DISTINCT FROM OLD.id_evaluacion) THEN
        PERFORM fn_verificar_score_total_evaluacion(NEW.id_evaluacion);
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER tg_evaluacion_score_total_consistente
    AFTER INSERT ON evaluacion
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_tg_evaluacion_score_total();

CREATE CONSTRAINT TRIGGER tg_detalle_evaluacion_score_total_consistente
    AFTER INSERT OR UPDATE OR DELETE ON detalle_evaluacion
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_tg_detalle_evaluacion_score_total();

-- =====================================================================================
-- Trigger B (CA03): la categoría guardada es exactamente la del rango de esa versión de política
-- que contiene el puntaje. Se comprueba al insertar (las actualizaciones las bloquea el trigger C).
-- Si falta algún dato de clasificación, decide ck_evaluacion_clasificacion_completa.
-- =====================================================================================
CREATE FUNCTION fn_tg_evaluacion_categoria_coherente() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    IF NEW.score_total IS NULL OR NEW.categoria IS NULL OR NEW.id_politica IS NULL THEN
        RETURN NEW;
    END IF;
    IF NOT EXISTS (
            SELECT 1
            FROM rango_clasificacion r
            WHERE r.id_politica = NEW.id_politica
              AND r.categoria = NEW.categoria
              AND NEW.score_total BETWEEN r.limite_inferior AND r.limite_superior) THEN
        RAISE EXCEPTION 'La categoria % no corresponde al puntaje % en la politica %',
            NEW.categoria, NEW.score_total, NEW.id_politica
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_evaluacion_categoria_coherente';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tg_evaluacion_categoria_coherente
    BEFORE INSERT ON evaluacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_evaluacion_categoria_coherente();

-- =====================================================================================
-- Trigger C (CA06): la clasificación registrada es inmutable. Bloquea cualquier cambio de
-- score_total, categoria o id_politica, incluida la asignación posterior a una evaluación
-- histórica (no se reclasifican, decisión 7). No afecta a las demás columnas.
-- =====================================================================================
CREATE FUNCTION fn_tg_evaluacion_clasificacion_inmutable() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    IF NEW.score_total IS DISTINCT FROM OLD.score_total
            OR NEW.categoria IS DISTINCT FROM OLD.categoria
            OR NEW.id_politica IS DISTINCT FROM OLD.id_politica THEN
        RAISE EXCEPTION 'La clasificacion de la evaluacion % es inmutable', OLD.id_evaluacion
            USING ERRCODE = 'object_not_in_prerequisite_state';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tg_evaluacion_clasificacion_inmutable
    BEFORE UPDATE OF score_total, categoria, id_politica ON evaluacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_evaluacion_clasificacion_inmutable();
