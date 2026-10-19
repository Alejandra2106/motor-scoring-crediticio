-- HU14: saga de recomendaciones. Estado de la evaluación y outbox transaccional.
-- Las evaluaciones anteriores a la saga conservan estado NULL y no pueden incorporarse a ella.
-- Sin restricciones NOT VALID: esta migración no bloquea las filas históricas.
-- Todas las funciones fijan search_path = esquema de la migración, pg_temp (como V8).
-- Compatibilidad con V8: el trigger A (AFTER INSERT) y el B (BEFORE INSERT) no se disparan al
-- actualizar estado; el C solo se dispara con UPDATE OF score_total, categoria, id_politica.

-- =====================================================================================
-- 1. Estado de la evaluación. CHECK válido: NULL (histórica) lo cumple.
-- =====================================================================================
ALTER TABLE evaluacion ADD COLUMN estado VARCHAR(10);

ALTER TABLE evaluacion
    ADD CONSTRAINT ck_evaluacion_estado CHECK (estado IN ('PENDIENTE', 'COMPLETADA'));

-- =====================================================================================
-- 2. Outbox. id_concesion identifica el intento vigente; concesion_hasta es su vencimiento.
-- =====================================================================================
CREATE TABLE trabajo_recomendacion (
    id_evaluacion       BIGINT       PRIMARY KEY,
    estado_trabajo      VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    intentos            INTEGER      NOT NULL,
    intentos_totales    INTEGER      NOT NULL,
    proximo_intento     TIMESTAMPTZ  NOT NULL,
    id_concesion        UUID         NULL,
    concesion_hasta     TIMESTAMPTZ  NULL,
    codigo_ultimo_error VARCHAR(50)  NULL,
    ultimo_error        VARCHAR(500) NULL,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp(),
    procesado_en        TIMESTAMPTZ  NULL,
    CONSTRAINT fk_trabajo_recomendacion_evaluacion
        FOREIGN KEY (id_evaluacion) REFERENCES evaluacion (id_evaluacion),
    CONSTRAINT ck_trabajo_recomendacion_estado
        CHECK (estado_trabajo IN ('PENDIENTE', 'PROCESADO', 'ERROR_PERMANENTE')),
    CONSTRAINT ck_trabajo_recomendacion_intentos
        CHECK (intentos >= 0 AND intentos_totales >= intentos),
    CONSTRAINT ck_trabajo_recomendacion_concesion
        CHECK ((id_concesion IS NULL) = (concesion_hasta IS NULL)),
    CONSTRAINT ck_trabajo_recomendacion_concesion_pendiente
        CHECK (estado_trabajo = 'PENDIENTE' OR id_concesion IS NULL),
    CONSTRAINT ck_trabajo_recomendacion_procesado
        CHECK ((estado_trabajo = 'PROCESADO') = (procesado_en IS NOT NULL)),
    CONSTRAINT ck_trabajo_recomendacion_error
        CHECK (estado_trabajo <> 'ERROR_PERMANENTE' OR codigo_ultimo_error IS NOT NULL)
);

CREATE INDEX ix_trabajo_recomendacion_elegibles
    ON trabajo_recomendacion (proximo_intento) WHERE estado_trabajo = 'PENDIENTE';

-- =====================================================================================
-- 3. I1: toda evaluación nueva se inserta PENDIENTE. AFTER INSERT: ve la fila final.
-- =====================================================================================
CREATE FUNCTION fn_tg_evaluacion_estado_inicial() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    IF NEW.estado IS DISTINCT FROM 'PENDIENTE' THEN
        RAISE EXCEPTION 'La evaluacion % debe registrarse en estado PENDIENTE', NEW.id_evaluacion
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ck_evaluacion_estado_inicial';
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER tg_evaluacion_estado_inicial
    AFTER INSERT ON evaluacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_evaluacion_estado_inicial();

-- =====================================================================================
-- 4. I2: NULL no cambia nunca; solo PENDIENTE -> COMPLETADA. AFTER UPDATE sin lista de columnas.
-- =====================================================================================
CREATE FUNCTION fn_tg_evaluacion_estado_transicion() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    IF NEW.estado IS NOT DISTINCT FROM OLD.estado
            OR (OLD.estado = 'PENDIENTE' AND NEW.estado = 'COMPLETADA') THEN
        RETURN NULL;
    END IF;
    RAISE EXCEPTION 'Transicion de estado no permitida en la evaluacion %: % -> %',
        OLD.id_evaluacion, OLD.estado, NEW.estado
        USING ERRCODE = 'object_not_in_prerequisite_state';
END;
$$;

CREATE TRIGGER tg_evaluacion_estado_transicion
    AFTER UPDATE ON evaluacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_evaluacion_estado_transicion();

-- =====================================================================================
-- 5. Creación del trabajo: PENDIENTE, con la concesión del primer intento en línea.
-- =====================================================================================
CREATE FUNCTION fn_tg_trabajo_recomendacion_inicial() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    IF NEW.estado_trabajo <> 'PENDIENTE'
            OR NEW.intentos <> 1
            OR NEW.intentos_totales <> 1
            OR NEW.id_concesion IS NULL
            OR NEW.concesion_hasta <= clock_timestamp() THEN
        RAISE EXCEPTION 'El trabajo de la evaluacion % debe crearse PENDIENTE, con un intento y una concesion vigente',
            NEW.id_evaluacion
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ck_trabajo_recomendacion_inicial';
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER tg_trabajo_recomendacion_inicial
    AFTER INSERT ON trabajo_recomendacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_trabajo_recomendacion_inicial();

-- =====================================================================================
-- 6. I4, I5, I7-I11: transiciones, concesiones y contadores del trabajo.
--    SECURITY INVOKER a propósito: current_user identifica a quien reactiva.
-- =====================================================================================
CREATE FUNCTION fn_tg_trabajo_recomendacion_transicion() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
DECLARE
    v_ahora             TIMESTAMPTZ := clock_timestamp();
    v_concesion_vigente BOOLEAN := OLD.id_concesion IS NOT NULL AND OLD.concesion_hasta >= v_ahora;
    v_propietario       NAME;
    v_motivo            TEXT;
BEGIN
    IF OLD.estado_trabajo = 'PROCESADO' THEN
        v_motivo := 'PROCESADO es terminal';
    ELSIF NEW.id_evaluacion <> OLD.id_evaluacion OR NEW.creado_en <> OLD.creado_en THEN
        v_motivo := 'id_evaluacion y creado_en son inmutables';

    ELSIF NEW.id_concesion IS NOT NULL AND NEW.id_concesion IS DISTINCT FROM OLD.id_concesion THEN
        -- Reclamo (I7)
        IF OLD.estado_trabajo <> 'PENDIENTE' OR NEW.estado_trabajo <> 'PENDIENTE' THEN
            v_motivo := 'solo se reclama un trabajo PENDIENTE';
        ELSIF v_concesion_vigente THEN
            v_motivo := 'la concesion actual sigue vigente';
        ELSIF NEW.intentos <> OLD.intentos + 1 OR NEW.intentos_totales <> OLD.intentos_totales + 1 THEN
            v_motivo := 'un reclamo suma exactamente un intento a ambos contadores';
        ELSIF NEW.concesion_hasta <= v_ahora THEN
            v_motivo := 'la concesion nueva debe vencer en el futuro';
        END IF;

    ELSIF NEW.intentos_totales <> OLD.intentos_totales THEN
        v_motivo := 'intentos_totales solo aumenta al reclamar';

    ELSIF NEW.id_concesion IS NOT NULL THEN
        -- Se conserva la misma concesión (I8)
        IF NEW.estado_trabajo <> OLD.estado_trabajo
                OR NEW.intentos <> OLD.intentos
                OR NEW.concesion_hasta <> OLD.concesion_hasta THEN
            v_motivo := 'una concesion no se modifica ni se prorroga: se libera o se sustituye';
        END IF;

    ELSIF OLD.id_concesion IS NOT NULL THEN
        -- Liberación por resultado o agotamiento (I9); OLD es PENDIENTE por el CHECK de concesión
        IF NEW.intentos <> OLD.intentos THEN
            v_motivo := 'liberar una concesion no cambia los intentos';
        END IF;

    ELSE
        -- Sin concesión antes ni después
        IF OLD.estado_trabajo = 'ERROR_PERMANENTE' AND NEW.estado_trabajo = 'PENDIENTE' THEN
            -- Reactivación (I5)
            SELECT pg_catalog.pg_get_userbyid(c.relowner) INTO v_propietario
            FROM pg_catalog.pg_class c WHERE c.oid = TG_RELID;
            IF current_user <> v_propietario THEN
                v_motivo := 'la reactivacion exige reactivar_trabajo_recomendacion';
            ELSIF NEW.intentos <> 0 THEN
                v_motivo := 'la reactivacion pone intentos a cero';
            END IF;
        ELSIF NEW.intentos <> OLD.intentos THEN
            v_motivo := 'intentos solo cambia al reclamar o al reactivar';
        ELSIF NEW.estado_trabajo = 'PROCESADO' THEN
            v_motivo := 'un exito solo se registra liberando una concesion';
        ELSIF OLD.estado_trabajo = 'ERROR_PERMANENTE' AND NEW.estado_trabajo <> 'ERROR_PERMANENTE' THEN
            v_motivo := 'transicion no permitida desde ERROR_PERMANENTE';
        END IF;
        -- Permitidos aquí: PENDIENTE -> PENDIENTE, PENDIENTE -> ERROR_PERMANENTE, ERROR -> ERROR
    END IF;

    IF v_motivo IS NOT NULL THEN
        RAISE EXCEPTION 'Modificacion no permitida del trabajo de la evaluacion %: %', OLD.id_evaluacion, v_motivo
            USING ERRCODE = 'object_not_in_prerequisite_state';
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER tg_trabajo_recomendacion_transicion
    AFTER UPDATE ON trabajo_recomendacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_trabajo_recomendacion_transicion();

-- =====================================================================================
-- 7. I13: sin borrado ni truncado (mismo patrón que V9).
-- =====================================================================================
CREATE FUNCTION fn_tg_trabajo_recomendacion_sin_borrado() RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
BEGIN
    RAISE EXCEPTION 'Los trabajos de recomendacion no pueden eliminarse'
        USING ERRCODE = 'object_not_in_prerequisite_state';
END;
$$;

CREATE TRIGGER tg_trabajo_recomendacion_sin_borrado
    BEFORE DELETE ON trabajo_recomendacion
    FOR EACH ROW EXECUTE FUNCTION fn_tg_trabajo_recomendacion_sin_borrado();

CREATE TRIGGER tg_trabajo_recomendacion_sin_truncate
    BEFORE TRUNCATE ON trabajo_recomendacion
    FOR EACH STATEMENT EXECUTE FUNCTION fn_tg_trabajo_recomendacion_sin_borrado();

-- =====================================================================================
-- 8. I3: consistencia del par sobre el estado FINAL, al confirmar. Bloqueo canónico:
--    evaluacion y después trabajo_recomendacion, ambos FOR SHARE. SECURITY DEFINER para que
--    las lecturas con bloqueo no dependan del rol que confirma; tablas cualificadas.
-- =====================================================================================
CREATE FUNCTION fn_tg_saga_consistente() RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
DECLARE
    v_id      BIGINT;
    v_estado  VARCHAR(10);
    v_trabajo VARCHAR(20);
    v_existe  BOOLEAN;
BEGIN
    IF TG_TABLE_NAME = 'evaluacion' THEN
        v_id := NEW.id_evaluacion;
    ELSIF TG_TABLE_NAME = 'trabajo_recomendacion' THEN
        v_id := NEW.id_evaluacion;
    ELSE
        RAISE EXCEPTION 'fn_tg_saga_consistente no admite la tabla %', TG_TABLE_NAME;
    END IF;

    SELECT e.estado INTO v_estado
    FROM ${flyway:defaultSchema}.evaluacion e
    WHERE e.id_evaluacion = v_id
    FOR SHARE;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    SELECT t.estado_trabajo INTO v_trabajo
    FROM ${flyway:defaultSchema}.trabajo_recomendacion t
    WHERE t.id_evaluacion = v_id
    FOR SHARE;
    v_existe := FOUND;

    IF (v_estado IS NULL AND v_existe)
            OR (v_estado = 'PENDIENTE' AND (NOT v_existe OR v_trabajo = 'PROCESADO'))
            OR (v_estado = 'COMPLETADA' AND (NOT v_existe OR v_trabajo <> 'PROCESADO')) THEN
        RAISE EXCEPTION 'Saga inconsistente en la evaluacion %: evaluacion=%, trabajo=%',
            v_id, v_estado, v_trabajo
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ck_evaluacion_saga_consistente';
    END IF;
    RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION fn_tg_saga_consistente() FROM PUBLIC;

CREATE CONSTRAINT TRIGGER tg_evaluacion_saga_consistente
    AFTER INSERT OR UPDATE OF estado ON evaluacion
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_tg_saga_consistente();

CREATE CONSTRAINT TRIGGER tg_trabajo_recomendacion_saga_consistente
    AFTER INSERT OR UPDATE OF estado_trabajo ON trabajo_recomendacion
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_tg_saga_consistente();

-- =====================================================================================
-- 9. I5: reactivación operativa. SECURITY DEFINER: dentro, current_user es el propietario,
--    única identidad que admite ERROR_PERMANENTE -> PENDIENTE. Bloqueo canónico.
--    Conserva intentos_totales y el último error como historial.
-- =====================================================================================
CREATE FUNCTION reactivar_trabajo_recomendacion(p_id_evaluacion BIGINT) RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
DECLARE
    v_estado VARCHAR(10);
BEGIN
    SELECT e.estado INTO v_estado
    FROM ${flyway:defaultSchema}.evaluacion e
    WHERE e.id_evaluacion = p_id_evaluacion
    FOR NO KEY UPDATE;
    IF NOT FOUND OR v_estado IS DISTINCT FROM 'PENDIENTE' THEN
        RAISE EXCEPTION 'La evaluacion % no existe o no esta PENDIENTE', p_id_evaluacion
            USING ERRCODE = 'object_not_in_prerequisite_state';
    END IF;

    UPDATE ${flyway:defaultSchema}.trabajo_recomendacion
       SET estado_trabajo  = 'PENDIENTE',
           intentos        = 0,
           proximo_intento = clock_timestamp(),
           actualizado_en  = clock_timestamp()
     WHERE id_evaluacion = p_id_evaluacion
       AND estado_trabajo = 'ERROR_PERMANENTE';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El trabajo de la evaluacion % no esta en ERROR_PERMANENTE', p_id_evaluacion
            USING ERRCODE = 'object_not_in_prerequisite_state';
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION reactivar_trabajo_recomendacion(BIGINT) FROM PUBLIC;
