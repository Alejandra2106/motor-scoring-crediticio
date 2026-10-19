-- scoring-api: propietario de las tablas sin privilegios de superusuario.
-- Hoy las tablas, secuencias y funciones de scoring-api pertenecen a un rol superusuario, que también usa la
-- aplicación. Este script crea un rol propietario (LOGIN, sin contraseña en este archivo, sin privilegios
-- administrativos) y le transfiere la propiedad de los objetos del esquema que pertenezcan a quien ejecuta el
-- script. No modifica datos. Las funciones SECURITY DEFINER de V11 pasan a ejecutarse como este propietario.
--
-- Orden: después de aplicar V10 y V11 y antes de roles-scoring-api.sql. Procedimiento completo en
-- docs/operacion/despliegue-v10-v11.md. Requiere respaldo verificado y autorización.
--
-- Uso: psql -h <host> -U <administrador> -d motor_scoring -v rol_propietario=<rol> -f propietario-scoring-api.sql
--   y, si ese rol va a ejecutar migraciones futuras: \password <rol>
-- Variables opcionales: esquema (por defecto public) y base_esperada (por defecto motor_scoring).

\set ON_ERROR_STOP on

\if :{?rol_propietario}
\else
    \echo 'Falta -v rol_propietario=<rol propietario de scoring-api>'
    \quit
\endif
\if :{?esquema}
\else
    \set esquema public
\endif
\if :{?base_esperada}
\else
    \set base_esperada motor_scoring
\endif

SELECT current_database() = :'base_esperada' AS base_correcta \gset
\if :base_correcta
\else
    \echo 'Conectado a una base distinta de la esperada; no se hace nada'
    \quit
\endif

BEGIN;

CREATE ROLE :"rol_propietario" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
COMMENT ON ROLE :"rol_propietario" IS 'scoring-api: propietario de las tablas; solo para migraciones';
GRANT CONNECT ON DATABASE :"base_esperada" TO :"rol_propietario";
GRANT USAGE, CREATE ON SCHEMA :"esquema" TO :"rol_propietario";

SELECT set_config('despliegue.esquema', :'esquema', true), set_config('despliegue.propietario', :'rol_propietario', true);

DO $$
DECLARE
    v_esquema     text := current_setting('despliegue.esquema');
    v_propietario text := current_setting('despliegue.propietario');
    r             record;
BEGIN
    -- Tablas y vistas (sus índices y secuencias de identidad cambian con ellas)
    FOR r IN
        SELECT format('%I.%I', n.nspname, c.relname) AS nombre, c.relkind
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = v_esquema
          AND c.relkind IN ('r', 'p', 'v', 'm')
          AND c.relowner = (SELECT oid FROM pg_roles WHERE rolname = current_user)
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e')
    LOOP
        EXECUTE format('ALTER %s %s OWNER TO %I',
            CASE r.relkind WHEN 'v' THEN 'VIEW' WHEN 'm' THEN 'MATERIALIZED VIEW' ELSE 'TABLE' END,
            r.nombre, v_propietario);
    END LOOP;
    -- Secuencias independientes (no ligadas a una columna)
    FOR r IN
        SELECT format('%I.%I', n.nspname, c.relname) AS nombre
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = v_esquema AND c.relkind = 'S'
          AND c.relowner = (SELECT oid FROM pg_roles WHERE rolname = current_user)
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype IN ('a', 'i', 'e'))
    LOOP
        EXECUTE format('ALTER SEQUENCE %s OWNER TO %I', r.nombre, v_propietario);
    END LOOP;
    -- Funciones (incluidas las de triggers y las SECURITY DEFINER)
    FOR r IN
        SELECT p.oid::regprocedure AS firma
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = v_esquema
          AND p.proowner = (SELECT oid FROM pg_roles WHERE rolname = current_user)
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid AND d.deptype = 'e')
    LOOP
        EXECUTE format('ALTER FUNCTION %s OWNER TO %I', r.firma, v_propietario);
    END LOOP;
END;
$$;

COMMIT;

-- Comprobación: objetos del esquema que siguen perteneciendo a otro rol (debe quedar vacío, salvo extensiones)
SELECT 'tabla' AS tipo, c.relname AS objeto, pg_get_userbyid(c.relowner) AS propietario
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = :'esquema' AND c.relkind IN ('r', 'p', 'v', 'm', 'S')
  AND pg_get_userbyid(c.relowner) <> :'rol_propietario'
  AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e')
UNION ALL
SELECT 'función', p.proname, pg_get_userbyid(p.proowner)
FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = :'esquema' AND pg_get_userbyid(p.proowner) <> :'rol_propietario'
  AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid AND d.deptype = 'e');
