# Despliegue controlado de V10, V11 y HU14 en `motor_scoring`

Estado: **ejecutado el 2026-10-19 sobre `motor_scoring`**, con autorización expresa, respaldo verificado y un
ensayo previo completo sobre una copia restaurada del respaldo (ver sección 5). Cualquier migración futura
exige de nuevo respaldo verificado, revisión del plan y autorización.

## 1. Qué cambia

| Migración | Efecto | Compatibilidad con V9 y los datos existentes |
|---|---|---|
| V10 `indices_consultas_evaluacion` | Dos índices: `detalle_evaluacion(id_evaluacion)` y `evaluacion(id_solicitante, fecha_evaluacion DESC, id_evaluacion DESC)` | Sin cambios de datos. `CREATE INDEX` (no concurrente) bloquea las escrituras de esas tablas mientras se construye; con el volumen actual (decenas de filas) es inmediato |
| V11 `saga_recomendaciones` | Columna `evaluacion.estado` (NULL, PENDIENTE, COMPLETADA) con su CHECK; tabla `trabajo_recomendacion` (outbox); triggers de estado inicial, transiciones, consistencia diferida y protección contra borrado; función `reactivar_trabajo_recomendacion` | Las evaluaciones existentes quedan con `estado` NULL (anteriores a la saga) y no se modifican. El CHECK admite NULL. Añadir una columna sin valor por defecto no reescribe la tabla |

Efectos a tener en cuenta:

- Desde V11, **toda evaluación nueva debe insertarse PENDIENTE junto con su trabajo** (triggers I1 e I3). Una
  versión de `scoring-api` anterior a HU14 ya no puede registrar evaluaciones: V11 solo se aplica junto con
  esta versión del código.
- Con la saga deshabilitada, o sin un catálogo de recomendaciones APROBADO, las evaluaciones nuevas quedan
  PENDIENTE: no aparecen en el historial y su explicación y sus recomendaciones responden 409.
- Las funciones `SECURITY DEFINER` fijan `search_path` y se revocan a `PUBLIC`. `reactivar_trabajo_recomendacion`
  queda reservada al propietario o a un administrador.
- Fallo parcial: en PostgreSQL, Flyway ejecuta cada migración en su propia transacción. Si V11 falla, se
  revierte entera (sin fila fallida en el historial) y la base queda en V10. Se corrige la causa y se repite;
  nunca se edita una migración ya aplicada. V10 y V11 no dependen de ningún cambio manual.

## 2. Roles y permisos

Situación actual: `scoring-api` y su Flyway usan el mismo rol, que **es superusuario**. La aplicación podría
desactivar los triggers que protegen la clasificación (V8, V9) y la saga (V11). Plan:

| Rol | Uso | Permisos |
|---|---|---|
| Propietario de `public` (migrador de `scoring-api`) | Solo durante la migración controlada (`SPRING_FLYWAY_USER`) | Propietario de las tablas, sin privilegios administrativos (`propietario-scoring-api.sql`) |
| Ejecución de `scoring-api` (`DB_USERNAME`) | En tiempo de ejecución | `roles-scoring-api.sql`: SELECT/INSERT/UPDATE donde escribe el código, sin DELETE ni TRUNCATE, SELECT sobre el historial de Flyway, sin EXECUTE de la reactivación |
| Migrador de `evaluation-query-api` | Propietario del esquema `consulta` | Crea las tablas de recomendaciones |
| Ejecución de `evaluation-query-api` | En tiempo de ejecución | SELECT sobre seis tablas de `public`; SELECT e INSERT sobre las de `consulta`; SELECT sobre su historial de Flyway |
| `auth-api` | En `motor_scoring` ya estaba separado: `auth_migrator` (propietario de `auth`) y `auth_api` (ejecución), ninguno superusuario | El despliegue dio a `auth_api` SELECT sobre el historial de Flyway (la aplicación ahora valida al arrancar) y le retiró DELETE, que el código no usa. Para entornos nuevos: `auth-api/docs/operacion/roles-auth-api.sql` |

Verificado en el ensayo: los roles de ejecución arrancan las aplicaciones, completan el flujo y reciben
`42501` al intentar escribir en scoring (lectura), modificar recomendaciones, escribir el historial de Flyway,
crear objetos, borrar detalles, truncar, desactivar triggers o ejecutar la reactivación.

## 3. Protección contra migraciones accidentales

`scoring-api` y `evaluation-query-api` usan un `FlywayMigrationStrategy` (`config/FlywayArranqueConfig`): por
defecto Flyway **solo valida**. Con migraciones pendientes la validación falla, la aplicación no arranca y no
se aplica nada. Ensayado: un arranque normal sobre un esquema en V9 terminó con error, la base siguió en V9 y no
se creó `trabajo_recomendacion`. Solo migra un arranque con `SCORING_FLYWAY_MIGRAR_AL_ARRANCAR=true` (o
`EVALUATION_QUERY_FLYWAY_MIGRAR_AL_ARRANCAR=true`). Nunca se define esa variable en la configuración
permanente de un entorno.

## 4. Procedimiento (cada paso, con autorización)

1. **Congelar**: detener `scoring-api` y `evaluation-query-api`; comprobar que no hay otras sesiones de escritura.
2. **Respaldo**: `pg_dump -Fc` de `motor_scoring` (incluye `public` y `auth`); verificar con `pg_restore --list`
   y guardar su SHA-256 fuera del repositorio. Sin respaldo verificado no se continúa.
3. **Lectura previa**: versión de Flyway (debe ser 9), conteos por tabla y huella de `auth`.
4. **Plan**: con la versión nueva, `flyway info` (o un arranque de validación) debe listar exactamente V10 y V11
   como pendientes.
5. **Migrar `scoring-api`**: un único arranque con `SCORING_FLYWAY_MIGRAR_AL_ARRANCAR=true`, `SPRING_FLYWAY_USER`
   y `SPRING_FLYWAY_PASSWORD` del propietario y la saga deshabilitada. Comprobar V11 en el historial y detener.
6. **Lectura posterior**: conteos idénticos, `estado` NULL en todas las evaluaciones existentes y huella de
   `auth` sin cambios.
7. **Roles**: `docs/operacion/roles-scoring-api.sql` y, en `evaluation-query-api`,
   `docs/operacion/01-preparar-antes-de-migrar.sql`. Asignar las contraseñas con `\password`.
8. **Migrar `evaluation-query-api`**: un arranque con `EVALUATION_QUERY_FLYWAY_MIGRAR_AL_ARRANCAR=true` y las
   credenciales del migrador de `consulta`; después, `02-permisos-despues-de-migrar.sql`.
9. **Arranque normal** de ambas API con los roles de ejecución y sin variables de migración. Habilitar la saga
   solo con un catálogo APROBADO por el negocio.
10. **Recuperación**: si algo falla antes del paso 9, se restaura el respaldo del paso 2 (`pg_restore` sobre
    una base limpia), previa autorización.

## 5. Ejecución del 2026-10-19 en `motor_scoring`

1. Lectura previa: V9 en `public`, V1 en `auth`, ninguna otra sesión; conteos solicitante/riesgo/regla/evaluación/
   detalle/usuario/token = 38/5/42/3/90/4/15; tablas de `public` propiedad de `postgres` (superusuario).
2. Respaldo `respaldos/2026-10-19-motor-scoring/motor_scoring-antes-v10-v11-roles-2026-10-19.dump` (fuera de los
   repositorios), SHA-256 `78a7c224849f1fd92645f42f1ea2440a31555bf08a45c46e6c5e56ec1846e5cb`, verificado
   restaurándolo en una base temporal: conteos, versión de Flyway y huella de las evaluaciones idénticos.
3. Ensayo completo sobre esa copia, con roles prefijados `ens_`: despliegue, permisos, arranque de las tres API y
   una evaluación con la saga y el catálogo aprobado (COMPLETADA, recomendaciones de la versión 1). La base de
   ensayo y sus roles se eliminaron.
4. Despliegue: un arranque sin la bandera terminó con error y dejó la base en V9; arranque controlado que aplicó
   V10 y V11; `propietario-scoring-api.sql` (`scoring_propietario`), `roles-scoring-api.sql` (`scoring_app`),
   `01-preparar-antes-de-migrar.sql` (`consulta_app`, `consulta_propietario`, esquema `consulta`), migración de
   la V1 de `evaluation-query-api`, `02-permisos-despues-de-migrar.sql` y el ajuste de `auth_api`.
5. Lectura posterior: `public` V1–V11 y `consulta` V1 con éxito; conteos idénticos; las 3 evaluaciones existentes
   con `estado` NULL; ningún objeto de `public`, `consulta` ni `auth` pertenece a un superusuario; 11
   comprobaciones de permisos con `SET ROLE`; las tres API arrancan solo validando Flyway.

Pendiente (la usuaria; nunca se escriben secretos en archivos ni en el repositorio):

- Contraseñas: `\password scoring_app`, `\password consulta_app` y, si van a ejecutar migraciones futuras,
  `\password scoring_propietario` y `\password consulta_propietario`.
- Configuración de las aplicaciones: `DB_USERNAME=scoring_app` en `scoring-api` y
  `EVALUATION_QUERY_DB_USERNAME=consulta_app` en `evaluation-query-api`, con sus contraseñas.
- Saga: generar el par RSA de servicio (README de `evaluation-query-api`), configurar las variables
  `SCORING_SERVICE_JWT_*` en ambas API y habilitar `SCORING_SAGA_HABILITADA` y
  `EVALUATION_QUERY_SAGA_HABILITADA`. Hasta entonces las evaluaciones nuevas quedan PENDIENTE y se completarán
  cuando se habilite.
- Datos: no hay ninguna política de clasificación publicada, y con las 42 reglas actuales un solicitante de
  prueba suma 425 puntos, fuera del rango [-100, 130] que exige HU11; esas evaluaciones se rechazan con 409
  `PUNTAJE_SIN_CLASIFICACION`. Revisar las reglas antes del uso (decisión de configuración del negocio).

