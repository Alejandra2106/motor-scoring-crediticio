# HU12 · Migración V8 (promovida)

La migración se validó en esquemas aislados de `motor_scoring_it` y se promovió el 2026-10-18 a
`src/main/resources/db/migration/V8__add_clasificacion_evaluacion.sql`. La migración activa es la
propuesta validada sin su cabecera de propuesta: las sentencias SQL son idénticas. Esta carpeta ya no
contiene ninguna migración. La propuesta original y su SHA-256 se conservan en
`build/respaldo-v8-promocion/`, que Git ignora.

Una vez promovida, cualquier arranque de la aplicación o prueba con Flyway activado sobre una base que
no la tenga aplicará V7 y V8. Su aplicación en `motor_scoring_it.public` (D2) y en `motor_scoring`
requiere autorizaciones separadas.

## Qué hace

| Elemento | Tipo | Propósito (HU12) |
|---|---|---|
| `evaluacion.score_total`, `categoria`, `id_politica` | Columnas, NULL permitido | CA05. Excepción aprobada a CLAUDE.md §12 para `score_total`. |
| `fk_evaluacion_politica` | Clave foránea, sin `ON DELETE` | Referencia a la versión exacta; una política referenciada no se puede eliminar (CA06). |
| `ck_evaluacion_categoria` | CHECK | Solo `BAJO`, `MEDIO`, `ALTO` (CA03). |
| `ck_evaluacion_clasificacion_completa` | CHECK `NOT VALID` | Toda evaluación nueva debe tener los tres datos; las históricas no se comprueban (decisión 7). |
| `ix_evaluacion_politica` | Índice | Consultas y comprobación de la clave foránea. |
| Trigger A: `tg_evaluacion_score_total_consistente`, `tg_detalle_evaluacion_score_total_consistente` | Constraint triggers diferidos (`INITIALLY DEFERRED`) | CA07/CA08: al confirmar, `score_total` = suma de `puntaje_obtenido`; si no, se revierte toda la transacción. Cubre insertar, modificar (incluido mover un detalle a otra evaluación) y eliminar detalles. |
| Trigger B: `tg_evaluacion_categoria_coherente` | `BEFORE INSERT` | CA03: la categoría es la del rango de esa versión de política que contiene el puntaje. |
| Trigger C: `tg_evaluacion_clasificacion_inmutable` | `BEFORE UPDATE OF score_total, categoria, id_politica` | CA06: la clasificación no cambia nunca, ni se asigna a evaluaciones históricas. |

Errores: A y B usan SQLSTATE `23514` (check_violation) y C usa `55000` (object_not_in_prerequisite_state).
Las funciones fijan `search_path` con el placeholder nativo `${flyway:defaultSchema}`.

Campo `constraint` de los errores (`RAISE ... USING CONSTRAINT`): identifica la restricción lógica
que falló. Es solo una etiqueta del error; no existe ningún objeto de base de datos con ese nombre.

| Etiqueta | Origen | Significado |
|---|---|---|
| `ck_evaluacion_score_total_consistente` | Trigger A (los dos constraint triggers comparten la función de verificación) | `score_total` no coincide con la suma de `puntaje_obtenido` de sus detalles. Se usa el prefijo `ck_` porque es una restricción lógica de `evaluacion` aplicada por dos triggers, no el nombre de uno de ellos. |
| `tg_evaluacion_categoria_coherente` | Trigger B | La categoría no corresponde al puntaje en esa versión de política. Coincide con el nombre del trigger, el único que la aplica. |

El trigger C no informa etiqueta: su error (`55000`) no es una violación de restricción. Ningún
código ni prueba del proyecto lee hoy el campo `constraint`; las pruebas comprueban el SQLSTATE y el
mensaje.

## Relación con la API (decisiones 1 y 2)

Los rechazos funcionales ocurren en la aplicación **antes** de escribir: sin política vigente responde
409 `POLITICA_VIGENTE_NO_DISPONIBLE` y con un puntaje sin categoría 409 `PUNTAJE_SIN_CLASIFICACION`, sin
guardar nada. Las restricciones y triggers de V8 son una segunda defensa: si algo los violara, la
transacción se revierte y la API respondería 500 `INTERNAL_ERROR` (no debería ocurrir por el flujo normal).

## Supuestos y riesgos

1. Los rangos de una política publicada no cambian (HU11 CA11, garantizado por la aplicación, no por
   la base). El trigger B compara contra los rangos actuales al insertar; una evaluación ya guardada no
   se vuelve a comprobar.
2. Nada de esto impide cambios por SQL directo con privilegios suficientes (por ejemplo, desactivar
   triggers o `SET CONSTRAINTS ... IMMEDIATE` no los elimina, pero un superusuario sí puede).
3. `ALTER TABLE evaluacion` toma un bloqueo exclusivo breve. Las columnas sin valor por defecto no
   reescriben la tabla.
4. Una vez aplicada V8, toda IT que levante el contexto JPA (incluida D2,
   `PoliticaClasificacionRepositoryAdapterIT`) necesita V8 en su esquema, porque Hibernate `validate`
   exige las columnas nuevas.
5. Las evaluaciones históricas devuelven `categoria`, `idPolitica` y `versionPolitica` en `null` en la API.
6. La restricción `NOT VALID` también se comprueba en los `UPDATE`: las evaluaciones históricas no pueden
   actualizarse en ninguna columna (la aplicación no las actualiza).

## Decisiones

- **P1 (2026-10-17):** SQL aprobado de forma condicional, sujeto a `EvaluacionV8MigracionIT`. La condición
  se cumplió el 2026-10-18 y V8 se promovió ese mismo día.
- **P2 (2026-10-17):** la inmutabilidad de `politica_clasificacion` y `rango_clasificacion` también se
  protegerá en la base, en una migración V9 independiente asociada a HU11. No implementada.
- **P3:** promoción hecha. Pendientes: D2 sobre `motor_scoring_it.public` y, por separado, `motor_scoring`.

## Pruebas de integración

Todas usan solo `motor_scoring_it` en `localhost:5432`, con `DB_USERNAME` y `DB_PASSWORD` del entorno.
Validan la configuración **sin conectarse** antes de abrir cualquier conexión: base dedicada exacta
terminada en `_it` (nunca `motor_scoring`), URL explícita con `-D` (nunca `DB_URL`) y esquema dedicado.
Cada una usa su propio esquema temporal; ninguna toca `public`. Ningún comando usa
`-Dsurefire.failIfNoSpecifiedTests=false`: un nombre de prueba incorrecto hace fallar Maven.
`ProteccionBaseIntegracion` exige la base y el esquema exactos, pero no fija el host ni el puerto: los
comandos los fijan en `localhost:5432`.

| Prueba | Esquema | Qué verifica | Resultado (2026-10-18) |
|---|---|---|---|
| `EvaluacionV8MigracionIT` | `hu12_v8_it` (lo crea; lo elimina solo si la base demuestra que lo creó esa ejecución) | V8 sobre V1-V7 con una evaluación histórica; columnas, restricciones y triggers A, B y C; histórica sin clasificación y no reclasificable; clave foránea. Solo admite, por igualdad exacta, la base `motor_scoring_it` y la URL `jdbc:postgresql://localhost:5432/motor_scoring_it`. | 13 pruebas, 0 fallos; esquema eliminado por la propia prueba |
| `ClasificacionEvaluacionIT` | `hu12_clasificacion_it` (no lo elimina) | API completa: 409 sin política y 409 sin categoría sin persistir; 201 con puntaje, categoría y versión persistidos; GET por id 200/404; solo ANALISTA (403 ADMIN, 401 sin token); un cambio de política no reclasifica. | 7 pruebas, 0 fallos; esquema eliminado después con limpieza autorizada |
| `EvaluacionRepositoryAdapterIT` | `hu12_it` (no lo elimina) | Adaptador JPA con las columnas de V8. | No ejecutada (opcional) |

Comandos (PowerShell, desde la raíz de `scoring-api`):

```powershell
.\mvnw.cmd -B test "-Dtest=EvaluacionV8MigracionIT" "-Dit.base-dedicada=motor_scoring_it" "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"

.\mvnw.cmd -B test "-Dtest=ClasificacionEvaluacionIT" "-Dspring.profiles.active=it" "-Dit.base-dedicada=motor_scoring_it" "-Dit.esquema-dedicado=hu12_clasificacion_it" "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it?currentSchema=hu12_clasificacion_it" "-Dspring.flyway.schemas=hu12_clasificacion_it"

.\mvnw.cmd -B test "-Dtest=EvaluacionRepositoryAdapterIT" "-Dspring.profiles.active=it" "-Dit.base-dedicada=motor_scoring_it" "-Dit.esquema-dedicado=hu12_it" "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it?currentSchema=hu12_it" "-Dspring.flyway.schemas=hu12_it"
```

Limpieza posterior de los esquemas que esas dos pruebas no eliminan (requiere autorización; solo afecta
a esos esquemas de `motor_scoring_it`). Antes de borrar, comprobar que el historial de Flyway del
esquema contiene su marca de creación (`type = 'SCHEMA'`):

```sql
DROP SCHEMA IF EXISTS hu12_clasificacion_it CASCADE;
DROP SCHEMA IF EXISTS hu12_it CASCADE;
```

La autorización por rol `ANALISTA` sin base de datos ya está cubierta por `SeguridadIntegracionTest` y
`EvaluacionControllerTest` (suite unitaria).
