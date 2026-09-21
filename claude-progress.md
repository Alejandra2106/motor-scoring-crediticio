# Progreso del proyecto — Motor de Scoring de Riesgo Crediticio

## Sprint 1

### Estado general

* El proyecto base está configurado (estructura de monolito modular con Package by Feature, Ports & Adapters por módulo, dependencias de Spring Boot, Maven Wrapper, integración continua básica).
* El Harness Engineering inicial está preparado (`CLAUDE.md` con las reglas de trabajo y `feature-list.json` con las HU del Sprint 1).
* Todas las HU del Sprint 1 se encuentran en estado **PENDIENTE**. Aún no se ha iniciado la implementación de ninguna.

---

### HU01 - Registrar solicitante

* **Estado**: COMPLETADA

* **Descripción breve**: Registrar un nuevo solicitante de crédito con su información personal y financiera, validarla y persistirla en la base de datos.

* **Criterios de aceptación**:

  * Registrar nombre completo, número de documento, ingresos mensuales, deudas mensuales, número de moras, historial crediticio y antigüedad laboral.
  * Validar los campos obligatorios y sus restricciones de formato y rango.
  * Validar que el número de documento no se encuentre registrado previamente.
  * Generar automáticamente un identificador único para el solicitante.
  * Persistir únicamente información previamente validada.
  * Retornar `HTTP 201` con la información de confirmación del registro.
  * La respuesta de creación contiene únicamente `idSolicitante`, `nombreCompleto`, `numeroDocumento` y `fechaRegistro`.
  * Rechazar solicitudes inválidas mediante respuestas `HTTP 400` con información sobre los campos inconsistentes.
  * Garantizar la unicidad del número de documento mediante una restricción de base de datos.
  * Garantizar la atomicidad de la operación mediante transacción y restricciones de integridad de PostgreSQL.
  * Generar o reutilizar un `X-Trace-Id` válido para la trazabilidad de cada solicitud.

* **Implementación**:

  * Endpoint `POST /api/v1/solicitantes` para registrar solicitantes.
  * Implementación organizada bajo el módulo `applicants`, utilizando Package by Feature y Ports & Adapters.
  * Entidad de dominio `Solicitante` sin dependencias de Spring ni JPA.
  * Enum de dominio `HistorialCrediticio` con los valores `BUENO`, `REGULAR` y `MALO`.
  * DTO `RegistrarSolicitanteRequest` con validaciones mediante Bean Validation.
  * `SolicitanteResponse` como DTO de salida con los cuatro campos definidos para la confirmación del registro.
  * Caso de uso `RegistrarSolicitanteUseCase` y servicio de aplicación `RegistrarSolicitanteService`.
  * Puerto de salida `SolicitanteRepositoryPort` y adaptador de persistencia mediante JPA.
  * Persistencia en PostgreSQL mediante Flyway como fuente de verdad del esquema.
  * Migración `V1__create_solicitante_table.sql` con restricciones de integridad, incluyendo unicidad del documento y validaciones de valores no negativos.
  * Configuración de Hibernate con `ddl-auto=validate`.
  * Operación de registro gestionada transaccionalmente mediante `@Transactional`.
  * Manejo centralizado de errores mediante `GlobalExceptionHandler`.
  * Implementación de trazabilidad mediante `TraceIdFilter`, MDC y header `X-Trace-Id`.
  * No se implementó autenticación ni autorización. RF16 queda documentado como dependencia funcional futura.

* **Pruebas**:

  * Pruebas unitarias del servicio de registro.
  * Pruebas de controlador mediante MockMvc.
  * Pruebas de validación de campos obligatorios y restricciones de formato y rango.
  * Pruebas de rechazo de documentos duplicados.
  * Pruebas de generación del identificador.
  * Pruebas de respuesta exitosa y estructura de `SolicitanteResponse`.
  * Pruebas de manejo global de errores.
  * Pruebas de trazabilidad mediante `X-Trace-Id` y MDC.
  * Prueba de integración de persistencia contra PostgreSQL local, verificando persistencia real, generación del ID, lectura de los datos persistidos, unicidad del documento y compatibilidad entre Flyway y la entidad JPA.
  * Verificación de compilación y ejecución de la suite de pruebas sin fallos.

* **Observaciones**:

  * RF16 - Controlar acceso queda fuera del alcance de HU01 y será implementado posteriormente mediante una Historia de Usuario específica de autenticación y autorización.
  * No se implementaron usuarios, roles, RBAC ni configuración adicional de Spring Security como parte de HU01.
  * La identificación del usuario que realiza la operación queda pendiente hasta la implementación de autenticación; la trazabilidad técnica mediante `X-Trace-Id`, fecha y hora sí está contemplada.

### HU02 - Consultar información del solicitante

* **Estado**: COMPLETADA
* **Descripción breve**: Consultar la información registrada de un solicitante.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**:
  * Endpoint `GET /api/v1/solicitantes/documento/{numeroDocumento}` para consultar un solicitante, con búsqueda exclusivamente por número de documento.
  * Respuesta de consulta con los campos definidos para HU02: `idSolicitante`, `nombreCompleto`, `numeroDocumento`, `ingresosMensuales`, `deudasMensuales`, `numeroMoras`, `historialCrediticio`, `antiguedadLaboral` y `fechaRegistro`.
  * Validación de entrada del número de documento mediante Bean Validation.
  * Respuestas HTTP: `200` (solicitante encontrado), `400` (número de documento con formato inválido) y `404` (solicitante no encontrado).
  * Manejo de errores centralizado en `GlobalExceptionHandler`, con `ApiError` consistente (incluye `traceId` igual al header `X-Trace-Id`, verificado manualmente y cubierto por prueba automatizada).
  * Reutilización de los componentes ya existentes de HU01 (`SolicitanteController`, `SolicitanteMapper`, `SolicitanteRepositoryPort`/`SolicitanteRepositoryAdapter`, `GlobalExceptionHandler`), extendidos sin duplicar lógica.
  * Implementación revisada mediante Pull Request y fusionada en `main` (commit de merge `8df519b`), a partir de los commits `ed790a6` (Implementar HU02: Consultar solicitante) y `cd6a5ae` (Ajustar HU02 según revisión).
* **Pruebas**:
  * Pruebas unitarias del caso de uso (`ConsultarSolicitanteServiceTest`).
  * Pruebas de controlador con MockMvc (`SolicitanteControllerTest`) cubriendo 200, 400, 404 y la consistencia entre `X-Trace-Id` y `traceId`.
  * Prueba automatizada de integración de persistencia (`SolicitanteRepositoryAdapterIT`), ejecutada con el perfil `test` y H2.
  * Verificación manual de la integración con PostgreSQL local, realizada mediante `curl` contra la aplicación en ejecución.
  * Suite completa ejecutada con `./mvnw.cmd clean test`: sin fallos.
* **Observaciones**: ninguna.

### HU03 - Crear variable de riesgo

* **Estado**: COMPLETADA
* **Descripción breve**: Crear una nueva variable de riesgo, seleccionada de un conjunto cerrado de cinco variables, con una descripción validada, para que quede disponible para la evaluación crediticia.
* **Criterios de aceptación**:
  * Permitir seleccionar la variable únicamente entre `INGRESOS_MENSUALES`, `NIVEL_ENDEUDAMIENTO`, `NUMERO_MORAS`, `HISTORIAL_CREDITICIO` y `ANTIGUEDAD_LABORAL`.
  * Registrar una descripción obligatoria, sin estar compuesta únicamente por espacios, de entre 10 y 255 caracteres.
  * Rechazar la creación si la variable no pertenece al conjunto permitido o si algún campo obligatorio no cumple sus validaciones.
  * Rechazar la creación si ya existe una variable de riesgo con el mismo nombre, informando la duplicidad.
  * Generar automáticamente el identificador de la variable y crearla en estado `ACTIVA`.
  * Persistir la variable únicamente tras superar todas las validaciones.
  * Registrar automáticamente la fecha y hora de creación (no enviable por el cliente).
  * Retornar `HTTP 201` con `idVariableRiesgo`, `variable`, `tipo`, `descripcion` y `estado`.
* **Implementación**:
  * Endpoint `POST /api/v1/variables-riesgo` para crear variables de riesgo.
  * Nuevo módulo `riskvariables`, organizado con Package by Feature y Ports & Adapters, siguiendo el mismo patrón ya usado en `applicants`.
  * Enum de dominio `NombreVariableRiesgo` con las cinco variables permitidas, cada una asociada a su `TipoVariableRiesgo` (`NUMERICO` o `CATEGORICO`); el tipo se deriva del enum en tiempo de uso y no se persiste como columna.
  * Enum de dominio `EstadoVariableRiesgo`, con únicamente el valor `ACTIVA` en esta HU (los estados adicionales quedan para HU04).
  * Entidad de dominio `VariableRiesgo` (sin dependencias de Spring ni JPA) con invariantes sobre la descripción (obligatoria, sin solo espacios, 10-255 caracteres tras recorte) y estado inicial `ACTIVA` fijado por el propio dominio (Creator).
  * `CrearVariableRiesgoUseCase` / `CrearVariableRiesgoService` (`@Transactional`), que valida la unicidad de la variable antes de guardar.
  * Puerto de salida `VariableRiesgoRepositoryPort` y adaptador de persistencia mediante JPA (`VariableRiesgoRepositoryAdapter`), que traduce una violación de integridad de PostgreSQL a `VariableRiesgoDuplicadaException`.
  * Migración `V2__create_riesgo_table.sql`: tabla `riesgo` con `id_riesgo`, `variable` (`NOT NULL`, `UNIQUE`, `CHECK` contra las cinco variables permitidas), `descripcion`, `estado` (`NOT NULL`, `DEFAULT 'ACTIVA'`, `CHECK (estado = 'ACTIVA')`) y `fecha_creacion` (`NOT NULL DEFAULT CURRENT_TIMESTAMP`).
  * `GlobalExceptionHandler` extendido de forma aditiva con el manejo de `VariableRiesgoDuplicadaException` (`HTTP 409`, código `VARIABLE_RIESGO_DUPLICADA`).
  * No se implementó autenticación, autorización, usuarios ni roles. RF09/RNF01 relacionados con la identificación del Administrador de riesgo quedan documentados como dependencia funcional futura, igual que RF16 de HU01. No se persiste ningún campo de usuario creador en esta HU.
* **Pruebas**:
  * Pruebas unitarias de dominio (`VariableRiesgoTest`): estado inicial `ACTIVA`, recorte y validación de la descripción, derivación del tipo desde la variable, reconstrucción de la entidad.
  * Pruebas de validación de Bean Validation del DTO de entrada (`CrearVariableRiesgoRequestValidationTest`).
  * Pruebas unitarias del servicio de aplicación (`CrearVariableRiesgoServiceTest`): creación exitosa y rechazo por variable duplicada.
  * Pruebas de controlador mediante MockMvc (`VariableRiesgoControllerTest`): 201 con exactamente los cinco campos aprobados, 400 por variable/descripción inválidas, 409 por duplicado, 500 genérico sin exponer detalles internos.
  * Prueba de integración de persistencia contra PostgreSQL local real (`VariableRiesgoRepositoryAdapterIT`, perfil `it`): generación de ID y `fecha_creacion`, lectura de todos los campos, restricción `UNIQUE` de `variable`, y verificación mediante SQL nativo de que las restricciones `CHECK` de `variable` y de `estado` viven en la base de datos.
  * Suite completa ejecutada con `./mvnw.cmd clean test`: 78 pruebas, sin fallos.
  * Prueba de integración ejecutada manualmente contra PostgreSQL local (`-Dtest=VariableRiesgoRepositoryAdapterIT -Dspring.profiles.active=it`): 7 pruebas, sin fallos.
* **Observaciones**:
  * RF09 y RNF01 relacionados con la identificación y autorización del Administrador de riesgo quedan fuera del alcance de HU03 y serán implementados mediante una HU de autenticación/autorización futura, siguiendo el mismo criterio ya aplicado en HU01 (RF16).
  * El nombre del campo `variable` en el request y en el response fue aprobado explícitamente como parte del contrato de la API, para su reutilización en HU04.
  * El enum `EstadoVariableRiesgo` se dejó deliberadamente con un único valor (`ACTIVA`); su extensión (por ejemplo `INACTIVA`) y la correspondiente restricción `CHECK` en base de datos quedan para HU04, mediante una nueva migración Flyway (sin modificar `V2__create_riesgo_table.sql`).

### HU04 - Gestionar estado de variable de riesgo

* **Estado**: COMPLETADA
* **Descripción breve**: Cambiar el estado de una variable de riesgo existente entre `ACTIVA` e `INACTIVA`, sin alterar el resto de su información asociada, para controlar su disponibilidad futura en el cálculo del score crediticio. No implementa reglas de scoring ni el cálculo del score en sí.
* **Criterios de aceptación**:
  * Identificar la variable de riesgo mediante `idRiesgo`; si no existe, informar que no fue encontrada sin realizar ningún cambio.
  * Restringir el estado a los valores `ACTIVA` e `INACTIVA`.
  * Permitir el cambio en ambos sentidos (`ACTIVA` → `INACTIVA` y `INACTIVA` → `ACTIVA`).
  * Preservar sin modificaciones el `idRiesgo`, la variable, la descripción y demás información asociada, así como las reglas de scoring que se implementen en HU futuras.
  * Persistir el nuevo estado únicamente tras validar la existencia de la variable y que el estado pertenezca al conjunto permitido.
  * Registrar la fecha y hora de la modificación.
  * Retornar `idRiesgo`, `estadoAnterior` y `estadoNuevo` tras un cambio exitoso.
  * La identificación del usuario responsable (Administrador de riesgo) queda como dependencia funcional futura de autenticación y autorización, igual que RF16 de HU01.
* **Implementación**:
  * Endpoint `PATCH /api/v1/variables-riesgo/{idRiesgo}/estado`, reutilizando el módulo `riskvariables` de HU03 (Package by Feature, Ports & Adapters).
  * Enum de dominio `EstadoVariableRiesgo` extendido con `INACTIVA` (antes solo `ACTIVA`).
  * Método de dominio `VariableRiesgo.cambiarEstado(EstadoVariableRiesgo)`, consistente con el diseño inmutable ya usado por `nueva`/`reconstruir`.
  * Nueva excepción de dominio `VariableRiesgoNoEncontradaException` (análoga a `SolicitanteNoEncontradoException`), manejada en `GlobalExceptionHandler` con `HTTP 404` y código `VARIABLE_RIESGO_NO_ENCONTRADA`.
  * `CambiarEstadoVariableRiesgoUseCase` / `CambiarEstadoVariableRiesgoService` (`@Transactional`), que registra en log estructurado (SLF4J + `traceId` del MDC existente) la fecha/hora, el estado anterior y el estado nuevo de cada cambio.
  * `VariableRiesgoRepositoryPort` extendido únicamente con `buscarPorId`; el método `guardar` existente de HU03 se reutilizó y adaptó para soportar también la actualización de una variable existente (sin agregar un método `actualizarEstado` separado).
  * `VariableRiesgoJpaEntity` extendida con la columna `fecha_modificacion` (nullable, se completa solo al cambiar el estado).
  * `GlobalExceptionHandler` extendido de forma aditiva también con el manejo de `MethodArgumentTypeMismatchException` (`HTTP 400`), necesario porque `idRiesgo` es el primer `@PathVariable` de tipo `Long` del proyecto.
  * Migración `V3__update_riesgo_estado.sql`: reemplaza la restricción `ck_riesgo_estado` para permitir `ACTIVA`/`INACTIVA` y agrega la columna `fecha_modificacion` (sin modificar `V1` ni `V2`).
  * No se implementó autenticación, autorización, RBAC, usuarios ni Spring Security. Tampoco se creó tabla de auditoría ni historial de estados: el estado anterior/nuevo de cada cambio se registra únicamente vía log estructurado, no persistido en base de datos.
* **Pruebas**:
  * Pruebas de dominio (`VariableRiesgoTest`): cambio `ACTIVA`→`INACTIVA` e `INACTIVA`→`ACTIVA` preservando los demás campos, rechazo de estado nulo.
  * Pruebas del servicio de aplicación (`CambiarEstadoVariableRiesgoServiceTest`): cambio exitoso en ambos sentidos, variable inexistente sin persistir cambios.
  * Pruebas de controlador (`VariableRiesgoControllerTest`): 200 con `idRiesgo`/`estadoAnterior`/`estadoNuevo`, 404 por variable inexistente, 400 por estado inválido, ausente y por `idRiesgo` no numérico.
  * Prueba de integración contra PostgreSQL local real (`VariableRiesgoRepositoryAdapterIT`, perfil `it`): persistencia real del cambio de estado en ambos sentidos, registro de `fecha_modificacion`, preservación de los demás campos, `buscarPorId`, variable inexistente, y verificación de que la restricción `ck_riesgo_estado` acepta `ACTIVA`/`INACTIVA` y sigue rechazando cualquier otro valor. Ejecutada dos veces de forma consecutiva contra la base real sin residuos: 12/12 pruebas, sin fallos.
  * Suite completa ejecutada con `./mvnw.cmd clean test`: 89 pruebas, sin fallos.
  * Gherkin: `docs/quality/HU04-gestionar-estado-variable-riesgo.feature`.
* **Observaciones**:
  * La antigua HU04 "Editar variable de riesgo" (edición de campos de una variable existente) fue **eliminada del backlog**; no forma parte del alcance de esta HU ni de ninguna otra pendiente en este documento.
  * La numeración HU05 no se utiliza para esta funcionalidad: "Gestionar estado de variable de riesgo" es, de forma definitiva, HU04.
  * RF16 de HU01 (control de acceso) y el criterio equivalente de esta HU (identificación del Administrador de riesgo) continúan como dependencia funcional futura de autenticación y autorización.

### HU05 - Crear regla de scoring

* **Estado**: COMPLETADA
* **Descripción breve**: Crear una nueva regla de scoring asociada a una variable de riesgo activa, estableciendo una condición (operador y valor) y un puntaje entre -100 y 100, para que quede disponible para el cálculo futuro del score crediticio. No implementa el cálculo del score ni la ejecución de reglas.
* **Criterios de aceptación**:
  * Recibir `idRiesgo`, `operador`, `valorCondicion` y `puntaje`; rechazar la creación si `idRiesgo` no corresponde a una variable de riesgo registrada (`HTTP 404`) o si esa variable está `INACTIVA` (`HTTP 409`).
  * Permitir únicamente los operadores `=`, `>`, `>=`, `<`, `<=`, restringidos a `=` cuando la variable asociada es categórica (`HISTORIAL_CREDITICIO`).
  * Validar `valorCondicion` según el tipo de la variable: numérico ≥ 0 (entero para `NUMERO_MORAS`, con decimales permitidos para las demás variables numéricas) o, para `HISTORIAL_CREDITICIO`, únicamente `BUENO`, `REGULAR` o `MALO`.
  * Validar `puntaje` como entero obligatorio entre -100 y 100 (aporte individual de la regla, no el score final).
  * Rechazar una regla duplicada por la combinación `idRiesgo + operador + valorCondicion` (`HTTP 409`), verificado tanto en la aplicación como mediante restricción `UNIQUE` en PostgreSQL.
  * Generar automáticamente `idRegla` y crear la regla en estado `ACTIVA`.
  * Persistir la regla únicamente tras superar todas las validaciones, de forma transaccional.
  * Retornar `HTTP 201` con `idRegla`, `idRiesgo`, `operador`, `valorCondicion` y `puntaje`.
* **Implementación**:
  * Endpoint `POST /api/v1/reglas-scoring` para crear reglas de scoring.
  * Nuevo módulo `scoring`, organizado con Package by Feature y Ports & Adapters, siguiendo el mismo patrón ya usado en `riskvariables`.
  * Enum de dominio `OperadorScoring` (serializado por su símbolo vía `@JsonCreator`/`@JsonValue`, igual convención de contrato JSON que los enums existentes) y `EstadoReglaScoring` (únicamente `ACTIVA` en esta HU, mismo criterio ya aplicado en `EstadoVariableRiesgo` en HU03).
  * Entidad de dominio `ReglaScoring` (sin dependencias de Spring ni JPA), responsable de validar la compatibilidad operador/tipo, el formato y rango de `valorCondicion` según el tipo de variable, y el rango de `puntaje`.
  * **Frontera entre módulos**: `scoring.domain` y `scoring.application` no importan ninguna clase de dominio ni el puerto de persistencia de `riskvariables`. Para RF01/RF02/RF05 se definió un puerto propio, `ConsultarVariableRiesgoPort` (con la proyección mínima `VariableRiesgoConsultada` y el enum propio `TipoVariable`), implementado por `VariableRiesgoConsultaAdapter` — el único componente de `scoring` que conoce el modelo de `riskvariables`, y que reutiliza internamente `VariableRiesgoRepositoryPort` ya existente sin duplicar persistencia. Decisión aprobada explícitamente por María Alejandra tras comparación de alternativas (ver historial de la conversación de HU05).
  * `CrearReglaScoringUseCase` / `CrearReglaScoringService` (`@Transactional`), que consulta la variable asociada, valida su existencia y estado, construye la regla de dominio y verifica la unicidad antes de persistir.
  * Puerto de salida `ReglaScoringRepositoryPort` y adaptador de persistencia mediante JPA (`ReglaScoringRepositoryAdapter`), que traduce una violación de integridad de PostgreSQL a `ReglaScoringDuplicadaException`. `id_riesgo` se persiste como columna simple (sin relación JPA `@ManyToOne` hacia `riskvariables`), preservando la misma frontera entre módulos también a nivel de persistencia.
  * Migración `V4__create_regla_scoring_table.sql`: tabla `regla_scoring` con `id_regla`, `id_riesgo` (FK hacia `riesgo`), `operador` (`CHECK` contra los cinco símbolos permitidos), `valor_condicion` (`VARCHAR`, validado en el dominio), `puntaje` (`CHECK BETWEEN -100 AND 100`), `estado` (`CHECK (estado = 'ACTIVA')`, mismo criterio que `V2` de `riesgo`), `fecha_creacion` y restricción `UNIQUE (id_riesgo, operador, valor_condicion)`.
  * `GlobalExceptionHandler` extendido de forma aditiva con el manejo de `ReglaScoringVariableNoEncontradaException` (`HTTP 404`), `ReglaScoringVariableInactivaException` (`HTTP 409`) y `ReglaScoringDuplicadaException` (`HTTP 409`).
  * No se implementó autenticación, autorización, usuarios ni roles. RF19/RNF01 quedan documentados como dependencia funcional futura, igual que en HU01/HU03/HU04. No se persiste ningún campo de usuario creador (RNF05 cubierto únicamente con `fecha_creacion`).
* **Pruebas**:
  * Pruebas unitarias de dominio (`ReglaScoringTest`, `OperadorScoringTest`): estado inicial `ACTIVA`, compatibilidad operador/tipo, validación numérica (incluyendo el caso especial entero de `NUMERO_MORAS`), validación categórica de `HISTORIAL_CREDITICIO`, rango de `puntaje`, reconstrucción.
  * Pruebas unitarias del adaptador de consulta cruzada (`VariableRiesgoConsultaAdapterTest`): mapeo de existencia, estado, tipo y `permiteDecimales` desde el dominio de `riskvariables` hacia la proyección propia de `scoring`.
  * Pruebas de validación de Bean Validation del DTO de entrada (`CrearReglaScoringRequestValidationTest`).
  * Pruebas unitarias del servicio de aplicación (`CrearReglaScoringServiceTest`): creación exitosa, variable inexistente, variable inactiva, regla duplicada, validación de dominio propagada.
  * Pruebas de controlador mediante MockMvc (`ReglaScoringControllerTest`): 201 con los cinco campos aprobados, 400 por datos inválidos, 404 por variable inexistente, 409 por variable inactiva y por duplicado, 500 genérico sin exponer detalles internos.
  * Prueba de integración de persistencia contra PostgreSQL local real (`ReglaScoringRepositoryAdapterIT`, perfil `it`): generación de `idRegla` y `fechaCreacion`, lectura de todos los campos, restricción `UNIQUE` de la combinación, `existeCombinacion`, y verificación mediante SQL nativo de que las restricciones `CHECK` de `operador` y `puntaje` y la `FK` hacia `riesgo` viven en la base de datos. La fixture de variables de riesgo usa un upsert idempotente (`ON CONFLICT ... DO UPDATE`) en vez de inserciones fijas, para convivir de forma segura con datos reales ya existentes en la base local (la tabla `riesgo` admite como máximo una fila por variable).
  * Suite completa ejecutada con `./mvnw.cmd clean test`: 162 pruebas, sin fallos.
  * Prueba de integración ejecutada manualmente contra PostgreSQL local (`-Dtest=ReglaScoringRepositoryAdapterIT -Dspring.profiles.active=it`): 8 pruebas, sin fallos. Flyway validó 4 migraciones y Hibernate `ddl-auto=validate` no reportó incompatibilidades.
  * Gherkin: `docs/quality/HU05-crear-regla-scoring.feature`.
* **Observaciones**:
  * RF19 y RNF01 relacionados con la autorización del Administrador de riesgo quedan fuera del alcance de HU05 y serán implementados mediante la HU de autenticación/autorización futura, siguiendo el mismo criterio ya aplicado en HU01/HU03/HU04.
  * El rango de `puntaje` (-100 a 100, inclusive) fue aprobado explícitamente por María Alejandra como aporte individual de una regla, no como rango del score final del sistema.
  * El mecanismo de comunicación entre `scoring` y `riskvariables` (puerto propio `ConsultarVariableRiesgoPort` + adaptador de traducción, en vez de reutilizar directamente el puerto interno de `riskvariables`) fue evaluado explícitamente comparando ambas alternativas contra SOLID/GRASP y aprobado como precedente para futuras dependencias entre módulos de negocio (por ejemplo, HU07 "Calcular score crediticio").

### HU06 - Editar regla de scoring

* **Estado**: PENDIENTE
* **Descripción breve**: Editar una regla de scoring existente.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.

### HU07 - Calcular score crediticio

* **Estado**: PENDIENTE
* **Descripción breve**: Calcular el score crediticio de un solicitante aplicando las reglas de scoring vigentes.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.
