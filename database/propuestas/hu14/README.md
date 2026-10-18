# HU14 · Migración V11 (propuesta, no promovida)

Estado de la evaluación (`PENDIENTE` / `COMPLETADA`) y outbox transaccional `trabajo_recomendacion` de la
saga de recomendaciones. Creada el 2026-10-18 como propuesta en esta carpeta: no está en
`src/main/resources/db/migration`, así que ni la aplicación ni la suite la aplican. Solo la usa
`TrabajoRecomendacionV11MigracionIT`, en un esquema temporal de `motor_scoring_it`.

Autorizado el 2026-10-18: la tabla `trabajo_recomendacion` en `public` (OBX) y este SQL como base de V11;
la barrera contra tokens antiguos en la aplicación (opción A); la política de reintentos ante interbloqueos
y la creación del trabajo siempre con la concesión del primer intento; la separación de roles, solo como
plan. La ejecución de la prueba de integración se autorizó y se realizó el 2026-10-18 (ver más abajo).
**No autorizado todavía:** promover la migración, aplicarla en cualquier base ni escribir el script de
permisos.

## Qué hace

| Elemento | Tipo | Propósito |
|---|---|---|
| `evaluacion.estado` + `ck_evaluacion_estado` | Columna NULL + CHECK válido | `PENDIENTE` o `COMPLETADA`; NULL = anterior a la saga. Sin `NOT VALID`: no bloquea filas históricas |
| `trabajo_recomendacion` | Tabla (clave primaria y foránea `id_evaluacion`) | Outbox: intentos, próximo intento, concesión (`id_concesion`, `concesion_hasta`), último error |
| `ix_trabajo_recomendacion_elegibles` | Índice parcial | Reclamo de trabajos `PENDIENTE` por `proximo_intento` |
| `tg_evaluacion_estado_inicial` | `AFTER INSERT` | I1: toda evaluación nueva, `PENDIENTE` |
| `tg_evaluacion_estado_transicion` | `AFTER UPDATE` | I2: NULL no cambia; solo `PENDIENTE → COMPLETADA` |
| `tg_trabajo_recomendacion_inicial` | `AFTER INSERT` | El trabajo nace `PENDIENTE`, con 1 intento y la concesión del primer intento en línea |
| `tg_trabajo_recomendacion_transicion` | `AFTER UPDATE`, `SECURITY INVOKER` | I4–I11: `PROCESADO` terminal, reclamo, concesiones, contadores, reactivación solo del propietario |
| `tg_trabajo_recomendacion_sin_borrado` / `_sin_truncate` | `BEFORE DELETE` / `BEFORE TRUNCATE` | I13 |
| `tg_evaluacion_saga_consistente`, `tg_trabajo_recomendacion_saga_consistente` | Constraint triggers diferidos, `SECURITY DEFINER` | I3: al confirmar, `NULL` ⇒ sin trabajo; `PENDIENTE` ⇒ trabajo `PENDIENTE`/`ERROR_PERMANENTE`; `COMPLETADA` ⇒ `PROCESADO`. Lecturas `FOR SHARE` |
| `reactivar_trabajo_recomendacion(bigint)` | Función `SECURITY DEFINER`, sin `EXECUTE` para `PUBLIC` | I5: `ERROR_PERMANENTE → PENDIENTE`, intentos a 0, conserva el historial |

Errores: `23514` (check_violation) para estado inicial, creación del trabajo y consistencia de la saga;
`55000` (object_not_in_prerequisite_state) para transiciones, concesiones, contadores y borrado.

## Compatibilidad con V8

Los triggers A (`AFTER INSERT`, diferido) y B (`BEFORE INSERT`) no se disparan al actualizar `estado`; el
C solo se dispara con `UPDATE OF score_total, categoria, id_politica`, columnas que JPA no actualiza
(`updatable = false`). El CHECK `NOT VALID` de V8 sigue bloqueando cualquier `UPDATE` de las evaluaciones
anteriores a V8 (limitación conocida; V12 aplazada).

## Riesgos y condiciones

1. **Roles.** Flyway de `scoring-api` usa el datasource de la aplicación (`application.properties` no define
   `spring.flyway.user`): la aplicación es la propietaria de las tablas. Mientras sea así, la protección de la
   reactivación (`current_user` = propietario) y los permisos por columna no tienen efecto. Separación de roles
   aprobada solo como plan; el propietario no debería ser superusuario (las funciones `SECURITY DEFINER` se
   ejecutan como él).
2. **Barrera contra tokens antiguos (I12):** la garantiza la aplicación (`WHERE id_concesion = :token`). La base
   solo garantiza que una concesión no se prorroga, que un éxito libera una concesión y los contadores.
3. **Orden canónico:** toda transacción que cambie `estado` o `estado_trabajo` bloquea primero la evaluación
   (`FOR NO KEY UPDATE`). Ningún trigger `BEFORE` debe cambiar `estado` ni `estado_trabajo`.
4. **Orden de versiones:** V10 (índices) debe aplicarse antes que V11 en cualquier base compartida; con
   `outOfOrder` desactivado, Flyway rechazaría V10 después de V11.
5. **Despliegue coordinado:** el trigger de estado inicial rechaza las inserciones del código actual (no informa
   `estado`). V11 solo puede aplicarse en una base compartida junto con el código de la saga, HU14 operativa en
   `evaluation-query-api`, la identidad S1 configurada y el catálogo `APROBADO`.
6. Un superusuario puede desactivar los triggers (`session_replication_role = replica`).

## Prueba de integración

`TrabajoRecomendacionV11MigracionIT`, esquema `hu14_v11_it` (lo crea; lo elimina solo si la base demuestra que
lo creó esa ejecución). Aplica V1–V7 con una evaluación anterior a V8, V8–V9 con una clasificada anterior a la
saga y V11. Cubre I1–I13, compatibilidad con V8, la reactivación por función, la limitación del propietario y
la concurrencia con dos conexiones (`SKIP LOCKED`, cambio concurrente sin orden canónico, serialización por el
orden canónico).

**Resultado (2026-10-18, ejecución autorizada):** PostgreSQL local `motor_scoring_it` (`localhost:5432`), solo el
esquema temporal `hu14_v11_it`, credenciales del entorno. **30 pruebas, 0 fallos, 0 errores**, `BUILD SUCCESS`.
El esquema temporal fue eliminado por la propia prueba: su limpieza hace fallar la ejecución si no puede
eliminarlo con seguridad, y no falló. Es el resultado que informa Maven; no se hizo ninguna consulta
independiente posterior para confirmar la eliminación del esquema. No se tocaron `public` ni `motor_scoring`.

Limitaciones que siguen abiertas:

1. **No está demostrado** que un rol de aplicación que no sea propietario de las tablas no pueda reactivar
   directamente un trabajo: la prueba se ejecuta con el rol propietario (crear un segundo rol no está
   autorizado). La prueba confirma, en cambio, que el propietario puede reactivar sin la función.
2. **La prueba B** (límite total de las llamadas HTTP del cliente de la saga) sigue aplazada hasta que exista una
   rama propia de HU14 en `scoring-api`.
3. **Las evaluaciones anteriores a V8** conservan la limitación documentada: el CHECK `NOT VALID` de V8 bloquea
   cualquier `UPDATE` de esas filas (V12 aplazada). La prueba lo confirma.

Ejecución (requiere autorización expresa; PowerShell, desde la raíz de `scoring-api`):

```powershell
.\mvnw.cmd -B test "-Dtest=TrabajoRecomendacionV11MigracionIT" "-Dit.base-dedicada=motor_scoring_it" "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
```
