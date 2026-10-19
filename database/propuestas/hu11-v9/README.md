# HU11 · Migración V9 (promovida)

Origen: decisión P2 (opción C, 2026-10-17): la inmutabilidad de las políticas publicadas (HU11 CA11)
se protege también en la base. Validada el 2026-10-18 en esquemas aislados de `motor_scoring_it`
(`PoliticaClasificacionV9MigracionIT`: 9 pruebas, 0 fallos; D2 con V9: 21 pruebas, 0 fallos) y
promovida ese mismo día a `src/main/resources/db/migration/V9__proteger_inmutabilidad_politica_clasificacion.sql`
sin su cabecera de propuesta (sentencias SQL idénticas). Esta carpeta ya no contiene ninguna
migración; la propuesta original y su SHA-256 se conservan en `build/respaldo-v9-promocion/`.
D2 se ejecuta ahora en un esquema temporal propio (opción 1 aprobada). Su aplicación en
`motor_scoring_it.public` y en `motor_scoring` requiere autorizaciones separadas.

## Qué hace

| Elemento | Tipo | Efecto |
|---|---|---|
| `fn_tg_politica_clasificacion_inmutable()` | Función de trigger | Rechaza la operación con SQLSTATE `55000` (object_not_in_prerequisite_state), igual que el trigger C de V8 |
| `tg_politica_clasificacion_inmutable` | `BEFORE UPDATE OR DELETE`, por fila | Ninguna versión publicada puede modificarse ni borrarse |
| `tg_rango_clasificacion_inmutable` | `BEFORE UPDATE OR DELETE`, por fila | Ningún rango puede modificarse ni borrarse |
| `tg_politica_clasificacion_sin_truncate`, `tg_rango_clasificacion_sin_truncate` | `BEFORE TRUNCATE`, por sentencia | Tampoco por `TRUNCATE`, que no dispara triggers por fila |

No se protege `configuracion_clasificacion_actual`: publicar una versión nueva actualiza su puntero.
No hace falta bloquear los `INSERT` de rangos (ver el comentario de la migración).

## Impacto

| Ámbito | Efecto | Acción necesaria |
|---|---|---|
| Código de producción | Ninguno: solo inserta políticas y rangos y actualiza la configuración; las entidades son `updatable = false` | — |
| `PoliticaClasificacionRepositoryAdapterIT` (D2) | Sus aserciones de rechazo siguen pasando (solo exigen una `SQLException`), pero **su limpieza** (`borrarPoliticaYRangosAtomicamente`) borra las políticas que crea en `motor_scoring_it.public` y fallaría | Adaptar D2 (ver "Decisión pendiente") |
| `EvaluacionV8MigracionIT` | Migra hasta la última versión y aplicaría V9; su prueba de la clave foránea borra antes los rangos y fallaría | Limitar su migración a `target("8")`: es la prueba de V8 |
| `ClasificacionEvaluacionIT`, `EvaluacionRepositoryAdapterIT` | Ninguno: no borran ni modifican políticas; sus esquemas temporales se eliminan con `DROP SCHEMA`, que no dispara estos triggers | — |
| Datos existentes | `motor_scoring` y `motor_scoring_it.public` no tienen políticas (2026-10-18); las que se publiquen quedarán inmutables | — |
| Correcciones | Una política errónea se corrige publicando una versión nueva; una corrección excepcional exige que el propietario desactive el trigger | Documentar el procedimiento |

## Decisión pendiente: cómo adaptar D2

| Opción | Descripción | Valoración |
|---|---|---|
| **1 (recomendada)** | Ejecutar D2 en un esquema temporal propio que se elimina entero al terminar (como `ClasificacionEvaluacionIT`), y eliminar su limpieza fila a fila | No debilita la V9; deja `motor_scoring_it.public` sin datos de prueba; exige adaptar la protección y la limpieza de D2 |
| 2 | Mantener D2 en `public` y desactivar los triggers solo durante su limpieza (`SET LOCAL session_replication_role = replica`, requiere superusuario) | Cambio pequeño, pero la prueba se salta la protección que se quiere garantizar |
| 3 | Añadir a la V9 una excepción por variable de sesión para permitir borrados | **No recomendada**: cualquiera con acceso SQL podría activarla y anularía la protección |

## Pruebas propuestas (no creadas)

- `PoliticaClasificacionV9MigracionIT` (esquema temporal `hu11_v9_it`, con la misma limpieza C3 que
  `EvaluacionV8MigracionIT`): aplica V1–V8, publica una política con sus rangos, aplica V9 y comprueba
  que `UPDATE`, `DELETE` y `TRUNCATE` sobre ambas tablas se rechazan con `55000`, que se puede publicar
  una versión nueva (INSERT) y actualizar `configuracion_clasificacion_actual`, y que las evaluaciones
  siguen pudiendo referenciar políticas.
- D2 y `EvaluacionV8MigracionIT` adaptadas según lo anterior.

## Recorrido (cada paso con autorización)

1. Revisión de este SQL y decisión sobre D2.
2. Crear y ejecutar `PoliticaClasificacionV9MigracionIT` en un esquema aislado; adaptar D2 y
   `EvaluacionV8MigracionIT`.
3. Promover V9 a `db/migration` y volver a ejecutar la suite y D2.
4. Aplicar en `motor_scoring_it.public` y, por separado y con respaldo previo, en `motor_scoring`.
