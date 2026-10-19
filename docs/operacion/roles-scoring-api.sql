-- scoring-api: rol de ejecución separado del propietario de las tablas (Fase 3 de la revisión de HU10–HU15).
-- Hoy scoring-api y su Flyway usan el mismo rol, que además es superusuario: la aplicación podría alterar o
-- desactivar los triggers de V8, V9 y V11 que protegen la clasificación y la saga. Este script crea un rol
-- de ejecución con solo los permisos que usa el código y deja las migraciones al rol propietario.
--
-- Se ejecuta DESPUÉS de aplicar V10 y V11 (necesita trabajo_recomendacion). NO se ha ejecutado sobre
-- motor_scoring: requiere respaldo verificado y autorización expresa (docs/operacion/despliegue-v10-v11.md).
-- Se ha verificado en esquemas temporales de motor_scoring_it.
--
-- Permisos (deducidos de los adaptadores de persistencia; sin DELETE ni TRUNCATE en ninguna tabla):
--   solicitante, detalle_evaluacion, politica_clasificacion, rango_clasificacion: SELECT, INSERT
--   riesgo, regla_scoring, evaluacion, trabajo_recomendacion: SELECT, INSERT, UPDATE
--   configuracion_clasificacion_actual: SELECT, UPDATE
--   flyway_schema_history: SELECT (la aplicación solo valida las migraciones al arrancar)
-- Sin EXECUTE sobre reactivar_trabajo_recomendacion (operación de soporte, propietario o administrador).
--
-- Uso: psql -h <host> -U <administrador> -d motor_scoring -v rol_app=<rol de ejecución> -f roles-scoring-api.sql
--   y después, de forma interactiva: \password <rol de ejecución>
-- Variables opcionales: esquema (por defecto public) y base_esperada (por defecto motor_scoring).
-- La aplicación se configura entonces con DB_USERNAME=<rol de ejecución> y, solo para un despliegue que
-- migre, SPRING_FLYWAY_USER / SPRING_FLYWAY_PASSWORD del rol propietario.

\set ON_ERROR_STOP on

\if :{?rol_app}
\else
    \echo 'Falta -v rol_app=<rol de ejecución de scoring-api>'
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

CREATE ROLE :"rol_app" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
COMMENT ON ROLE :"rol_app" IS 'scoring-api: rol de ejecución, sin propiedad de las tablas';
GRANT CONNECT ON DATABASE :"base_esperada" TO :"rol_app";
GRANT USAGE ON SCHEMA :"esquema" TO :"rol_app";

GRANT SELECT, INSERT ON :"esquema".solicitante, :"esquema".detalle_evaluacion,
    :"esquema".politica_clasificacion, :"esquema".rango_clasificacion TO :"rol_app";
GRANT SELECT, INSERT, UPDATE ON :"esquema".riesgo, :"esquema".regla_scoring, :"esquema".evaluacion,
    :"esquema".trabajo_recomendacion TO :"rol_app";
GRANT SELECT, UPDATE ON :"esquema".configuracion_clasificacion_actual TO :"rol_app";
GRANT SELECT ON :"esquema".flyway_schema_history TO :"rol_app";
GRANT USAGE ON ALL SEQUENCES IN SCHEMA :"esquema" TO :"rol_app";

COMMIT;

SELECT table_name, string_agg(privilege_type, ', ' ORDER BY privilege_type) AS privilegios
FROM information_schema.role_table_grants
WHERE grantee = :'rol_app' AND table_schema = :'esquema'
GROUP BY table_name
ORDER BY table_name;
