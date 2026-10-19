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
  * Prueba automatizada de integración de persistencia (`SolicitanteRepositoryAdapterIT`), ejecutada con el perfil `test` y H2. (Nota del 2026-10-18: el código actual de esa clase usa `@ActiveProfiles("it")`, es decir, PostgreSQL, y desde HU12 está protegida por `ProteccionBaseIntegracion`; no se pudo verificar con qué perfil se ejecutó originalmente.)
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

* **Estado**: COMPLETADA
* **Descripción breve**: Editar el operador, el valorCondicion y el puntaje de una regla de scoring existente, preservando su `idRegla`, `idRiesgo`, `estado` y `fechaCreacion`, para mantener actualizado el criterio utilizado posteriormente en el cálculo del score crediticio. No implementa el cálculo del score en sí (HU07).
* **Criterios de aceptación**:
  * Identificar la regla mediante `idRegla`; si no existe, `HTTP 404` sin realizar ninguna modificación.
  * Rechazar la edición si la variable de riesgo asociada está `INACTIVA` (`HTTP 409`), reutilizando la misma validación de HU05.
  * Validar `operador`, `valorCondicion` (según tipo de variable) y `puntaje` con las mismas reglas de dominio de HU05, sin duplicar la semántica de operadores ni de validación de valores.
  * Permitir editar una regla conservando su propia combinación `idRiesgo + operador + valorCondicion` (no debe autodetectarse como duplicado contra sí misma).
  * Rechazar la edición si la nueva combinación duplica la de otra regla existente (`HTTP 409`), verificado tanto en la aplicación (`existeCombinacion` con exclusión del propio `idRegla`) como mediante la restricción `UNIQUE` de PostgreSQL.
  * El `idRiesgo` y el `estado` de la regla no pueden modificarse mediante este endpoint.
  * Persistir la edición únicamente tras superar todas las validaciones, de forma transaccional.
  * Retornar `HTTP 200` con `idRegla`, `idRiesgo`, `operador`, `valorCondicion` y `puntaje` actualizados.
* **Implementación**:
  * Endpoint `PATCH /api/v1/reglas-scoring/{idRegla}`, reutilizando el módulo `scoring` de HU05 (Package by Feature, Ports & Adapters).
  * `EditarReglaScoringRequest` (sin `idRiesgo`, que no es editable) y `EditarReglaScoringUseCase`/`EditarReglaScoringService` (`@Transactional`), que consulta la regla existente, valida la variable asociada vía `ConsultarVariableRiesgoPort` (mismo puerto de HU05, sin duplicarlo), construye la regla editada mediante el nuevo método de dominio `ReglaScoring.editar(...)` y verifica unicidad antes de persistir.
  * Método de dominio `ReglaScoring.editar(OperadorScoring, String, Integer, TipoVariable, boolean)`, consistente con el diseño inmutable ya usado por `nueva`/`reconstruir`: retorna una nueva instancia preservando `idRegla`, `idRiesgo`, `estado` y `fechaCreacion`.
  * Nueva excepción de dominio `ReglaScoringNoEncontradaException` (análoga a `VariableRiesgoNoEncontradaException`/`SolicitanteNoEncontradoException`), manejada en `GlobalExceptionHandler` con `HTTP 404` y código `REGLA_SCORING_NO_ENCONTRADA`.
  * `ReglaScoringRepositoryPort` extendido con `buscarPorId` y una sobrecarga de `existeCombinacion` que excluye el propio `idRegla`; `ReglaScoringRepositoryAdapter.guardar` extendido para soportar también la actualización de una regla existente (mismo criterio que `VariableRiesgoRepositoryAdapter` en HU04: sin método `actualizar` separado).
  * No se requirió ninguna migración Flyway nueva: la edición reutiliza el esquema de `regla_scoring` ya creado por `V4__create_regla_scoring_table.sql` (sin modificarla).
  * No se implementó autenticación, autorización, usuarios ni roles. RNF01 (identificación del Administrador de riesgo) queda documentado como dependencia funcional futura, igual que en HU01/HU03/HU04/HU05.
* **Pruebas**:
  * Pruebas unitarias de dominio (`ReglaScoringTest`): `editar` preservando `idRegla`/`idRiesgo`/`estado`/`fechaCreacion`, conservación de la combinación propia, rechazo de operador incompatible con variable categórica, valor no numérico, valor categórico fuera de catálogo y puntaje fuera de rango.
  * Pruebas de validación de Bean Validation del DTO de entrada (`EditarReglaScoringRequestValidationTest`): 8 pruebas.
  * Pruebas unitarias del servicio de aplicación (`EditarReglaScoringServiceTest`): edición exitosa, regla inexistente, variable asociada inexistente, variable inactiva, combinación duplicada, conservación de la propia combinación, operador incompatible con variable categórica: 7 pruebas.
  * Pruebas de controlador mediante MockMvc (`ReglaScoringControllerTest`, compartido con HU05): 200 con los cinco campos actualizados, 400 por datos inválidos (incluido `idRegla` no numérico), 404 por regla inexistente, 409 por variable inactiva y por duplicado.
  * Prueba de integración de persistencia contra PostgreSQL local real (`ReglaScoringRepositoryAdapterIT`, perfil `it`): actualización de una regla existente preservando `idRegla`/`idRiesgo`/`estado`/`fechaCreacion`, verificación mediante lectura directa de la entidad JPA, `existeCombinacion` con exclusión permitiendo la propia combinación y detectando duplicado contra otra regla, y verificación mediante SQL nativo de que la restricción `UNIQUE` de la base de datos rechaza un `UPDATE` que duplique otra fila existente (defensa en profundidad más allá de la validación de la aplicación).
  * Suite completa ejecutada con `./mvnw.cmd test`: sin fallos (`EditarReglaScoringServiceTest` 7/7, `EditarReglaScoringRequestValidationTest` 8/8, `ReglaScoringControllerTest` 22/22 incluyendo HU05, `ReglaScoringTest` 55/55 incluyendo HU05 y las pruebas de evaluación agregadas por HU07).
  * Gherkin: `docs/quality/HU06-editar-regla-scoring.feature`.
* **Observaciones**:
  * RNF01 relacionado con la autorización del Administrador de riesgo queda fuera del alcance de HU06 y será implementado mediante la HU de autenticación/autorización futura, siguiendo el mismo criterio ya aplicado en HU01/HU03/HU04/HU05.
  * Implementada y verificada mediante Pull Request y fusionada en `main` (commit de merge `993d9d7`, a partir del commit `fe4a62a` "Implementar HU06: Editar regla de scoring").
  * Esta sección de `claude-progress.md` estaba desactualizada (`PENDIENTE`/"pendiente de documentar") pese a que la HU ya estaba completa y fusionada; se corrigió a solicitud explícita de María Alejandra.

### HU07 - Calcular score crediticio

* **Estado**: COMPLETADA (pendiente de commit/push y de revisión final por María Alejandra)
* **Descripción breve**: Calcular el score crediticio de un solicitante identificado por `idSolicitante` o `numeroDocumento`, evaluando las reglas de scoring `ACTIVA` asociadas a variables de riesgo `ACTIVA` contra sus datos, y registrar la evaluación junto con el detalle (snapshot histórico) de cada regla considerada.
* **Criterios de aceptación**:
  * Identificar al solicitante por exactamente uno de `idSolicitante` o `numeroDocumento`; ambos o ninguno es `HTTP 400`; solicitante inexistente es `HTTP 404` y no crea evaluación.
  * Consultar únicamente variables de riesgo `ACTIVA` y, dentro de ellas, únicamente reglas de scoring `ACTIVA`.
  * Evaluar cada regla considerada comparando el valor real del solicitante contra `operador`/`valorCondicion`, reutilizando la semántica de operadores de HU05/HU06 (sin duplicarla).
  * `NIVEL_ENDEUDAMIENTO` se calcula como `(deudas_mensuales / ingresos_mensuales) × 100` (D1); si `ingresos_mensuales = 0` y existe al menos una regla `ACTIVA` sobre esa variable, el cálculo se bloquea completamente con `HTTP 409`, sin crear evaluación ni detalles (D8).
  * El score total es la suma de `puntajeObtenido` de todas las reglas consideradas (cumplidas y no cumplidas); no se persiste como columna de `EVALUACION` (RF07/CLAUDE.md §12).
  * Se registran en `DETALLE_EVALUACION` **todas** las reglas consideradas, cumplidas o no (D3), con snapshot de `operadorAplicado`, `valorCondicionAplicado` y `puntajeReglaAplicado` tal como estaban en el momento del cálculo (D2), para que una edición posterior de la regla (HU06, que la modifica en el mismo registro) no altere el histórico.
  * Persistencia atómica de `EVALUACION` + todos sus `DETALLE_EVALUACION` (`@Transactional` en la capa de aplicación, respaldado por las restricciones e integridad transaccional de PostgreSQL).
  * No se agregó estado `INACTIVA` para `regla_scoring` (D4); no se modificó el modelo de `SOLICITANTE` (D6); autenticación/autorización quedan fuera de alcance (D7), igual que en HU01-HU06.
* **Implementación**:
  * Endpoint `POST /api/v1/evaluaciones`, nuevo módulo `evaluations` (Package by Feature, Ports & Adapters), dueño de la orquestación pero sin absorber reglas de negocio de `scoring`.
  * Siguiendo el precedente de HU05: `evaluations` no importa clases de dominio de `applicants`, `riskvariables` ni `scoring` en sus capas `domain`/`application`. Se definieron tres puertos de salida propios con sus proyecciones (`ConsultarSolicitanteEvaluacionPort`/`SolicitanteParaEvaluacion`, `ConsultarVariablesActivasEvaluacionPort`/`VariableActivaEvaluada`, `ReglasScoringConsultaPort`/`ResultadoReglaEvaluada`), cada uno implementado por un único adapter de traducción en `evaluations.infrastructure.output` que sí conoce el módulo externo, reutilizando sus puertos de persistencia existentes sin duplicarlos.
  * Extensiones aditivas (sin modificar comportamiento existente, solo métodos nuevos) a módulos ya existentes: `SolicitanteRepositoryPort`/`SolicitanteRepositoryAdapter` (+`buscarPorId`), `VariableRiesgoRepositoryPort`/`VariableRiesgoRepositoryAdapter`/`VariableRiesgoJpaRepository` (+`listarActivas`), `ReglaScoringRepositoryPort`/`ReglaScoringRepositoryAdapter`/`ReglaScoringJpaRepository` (+`listarActivasPorRiesgos`).
  * Lógica de evaluación de una regla (RF06) agregada al dominio de `scoring` por Information Expert, sin introducir Strategy (evaluado explícitamente y descartado por no haber variación de comportamiento que lo justifique, según CLAUDE.md §6/§13 de la especificación): `OperadorScoring.comparar(int)` interpreta el resultado de un `compareTo` según el símbolo; `ReglaScoring.evaluar(String valorReal, TipoVariable tipoVariable)` compara numérica o categóricamente reutilizando esa lógica.
  * Fórmula de `NIVEL_ENDEUDAMIENTO` y bloqueo D8 encapsulados en `evaluations.domain.ValorVariableResolver` (única responsabilidad: resolver el valor real de una variable a partir de la proyección propia del solicitante), con validación defensiva de RF05 (actualmente inalcanzable con el esquema vigente, dado que los campos financieros de `SOLICITANTE` son `NOT NULL` desde HU01).
  * Entidades de dominio inmutables `Evaluacion` (agrega `getScoreTotal()` sumando los `puntajeObtenido` de sus `DetalleEvaluacion`) y `DetalleEvaluacion` (valida como invariante que `puntajeObtenido` sea consistente con `condicionCumplida` y `puntajeReglaAplicado`), sin dependencias de Spring ni JPA.
  * `CalcularScoreService` (`@Transactional`): identifica al solicitante, consulta variables activas, determina qué variables tienen reglas vigentes (evitando resolver `NIVEL_ENDEUDAMIENTO`, y por tanto el bloqueo D8, cuando esa variable no tiene ninguna regla activa asociada), evalúa y persiste.
  * Persistencia mediante `EvaluacionRepositoryPort`/`EvaluacionRepositoryAdapter`, con `EvaluacionJpaEntity`/`DetalleEvaluacionJpaEntity` usando columnas simples (`id_solicitante`, `id_regla` como `Long`, sin relaciones JPA `@ManyToOne`), igual criterio que `regla_scoring` en HU05 para preservar la frontera entre módulos también a nivel de persistencia.
  * Migración `V5__create_evaluacion_tables.sql`: tablas `evaluacion` (`id_evaluacion`, `id_solicitante` con FK a `solicitante`, `fecha_evaluacion`) y `detalle_evaluacion` (`id_detalle_evaluacion`, `id_evaluacion` con FK, `id_regla` con FK a `regla_scoring`, `operador_aplicado`/`valor_condicion_aplicado`/`puntaje_regla_aplicado` como snapshot, `condicion_cumplida`, `puntaje_obtenido`), sin modificar V1-V4.
  * `GlobalExceptionHandler` extendido de forma aditiva con `SolicitanteEvaluacionNoEncontradoException` (`HTTP 404`, código `SOLICITANTE_NO_ENCONTRADO`, tipo propio de `evaluations` distinto del homónimo de `applicants`, mismo criterio que HU05 con `ReglaScoringVariableNoEncontradaException`/`VariableRiesgoNoEncontradaException`) y `NivelEndeudamientoIndeterminadoException` (`HTTP 409`, código `NIVEL_ENDEUDAMIENTO_INDETERMINADO`).
  * `CalcularScoreRequest` valida "exactamente uno de `idSolicitante`/`numeroDocumento`" mediante `@AssertTrue` (reutiliza el mecanismo existente de `MethodArgumentNotValidException` → `HTTP 400 VALIDATION_ERROR`, sin código nuevo en el manejador global).
  * Respuesta (`EvaluacionResponse`/`DetalleEvaluacionResponse`) limitada a `idEvaluacion`, `idSolicitante`, `fechaEvaluacion`, `scoreTotal` y, por detalle, `idRegla`/`operadorAplicado`/`valorCondicionAplicado`/`condicionCumplida`/`puntajeObtenido`; no expone datos financieros del solicitante.
  * `EvaluacionController` documentado con las mismas convenciones springdoc/`@Operation`/`@ApiResponses` ya usadas en `ReglaScoringController`; no existe (ni se creó) un contrato OpenAPI estático versionado independiente.
  * Gherkin: `docs/quality/HU07-calcular-score-crediticio.feature` (incluye el escenario de D8 marcado `@pendiente-decision-negocio` ya resuelto por la decisión aprobada).
  * No se implementó autenticación, autorización, RBAC, usuarios ni Spring Security (RF14/RNF02 documentados como dependencia funcional futura, igual que en HU01-HU06). No se agregó `INACTIVA` a `regla_scoring`. No se modificaron `pom.xml`, `.github/workflows/ci.yml`, `CLAUDE.md`, `feature-list.json`, `CreditScoringEngineApplication.java` ni las migraciones V1-V4.
* **Pruebas**:
  * Dominio: `OperadorScoringTest`/`ReglaScoringTest` extendidos con pruebas de `comparar`/`evaluar` por cada operador y tipo de variable; nuevos `EvaluacionTest`, `DetalleEvaluacionTest` (incluida la invariante de consistencia de `puntajeObtenido`) y `ValorVariableResolverTest` (fórmula D1, bloqueo D8, validación defensiva RF05).
  * Aplicación: `CalcularScoreServiceTest` (identificación por ID/documento, solicitante inexistente, sin variables activas, variable activa sin reglas, múltiples reglas cumplidas/no cumplidas, resolución perezosa del valor real solo para variables con reglas vigentes, propagación de `NivelEndeudamientoIndeterminadoException` sin persistir, `idSolicitante` correcto al guardar) y `CalcularScoreRequestValidationTest` (exactamente uno de los dos identificadores).
  * Adaptadores de traducción: `SolicitanteConsultaAdapterTest`, `VariablesActivasConsultaAdapterTest`, `ReglasScoringConsultaAdapterTest` (numérica, categórica, cumplida/no cumplida).
  * Controlador: `EvaluacionControllerTest` (201, 400 por ambos/ningún identificador y formato inválido, 404, 409, 500 genérico sin exponer detalles, `X-Trace-Id`).
  * Integración de persistencia contra PostgreSQL local real (`EvaluacionRepositoryAdapterIT`, perfil `it`): generación de IDs y `fechaEvaluacion`, persistencia de todos los detalles con su snapshot, preservación histórica tras editar la regla original (usando el mismo `EntityManager`/transacción de la prueba para evitar el autobloqueo por el lock `FOR KEY SHARE` que una FK entrante mantiene sobre `regla_scoring` hasta el fin de la transacción), y rechazo por FK inexistente verificando además que la evaluación no queda visible para otra conexión (atomicidad). 5/5 pruebas, sin fallos.
  * Suite completa ejecutada con `./mvnw.cmd test`: 264 pruebas, sin fallos.
* **Observaciones**:
  * Durante la implementación se detectaron y corrigieron tres problemas reales en el fixture de `EvaluacionRepositoryAdapterIT` (no en el código de producción): un autobloqueo (self-deadlock) entre dos conexiones JDBC de la misma prueba por un lock `FOR KEY SHARE` de PostgreSQL sobre una fila de `regla_scoring` referenciada por FK, y dos colisiones de la restricción `UNIQUE` de `regla_scoring` por sufijos de unicidad (`System.nanoTime() % 1000`) con muy poca entropía frente a una base de datos real persistente entre ejecuciones.
  * `feature-list.json` también tiene a HU05 y HU06 marcadas como `pending` pese a estar ya fusionadas en `main` según el historial de git; no se modificó por estar fuera del alcance autorizado para esta actualización (solo `claude-progress.md`).
  * Contrato ampliado por HU12 (2026-10-17, autorizado): `POST /api/v1/evaluaciones` conserva el 201 y sus campos, agrega `categoria`, `idPolitica` y `versionPolitica`, y responde 409 `POLITICA_VIGENTE_NO_DISPONIBLE` o 409 `PUNTAJE_SIN_CLASIFICACION` sin persistir. Ver la sección de HU12.
  * Pendiente de autorización explícita: commit y push (no realizados).

### HU11 - Gestionar políticas de clasificación

* **Estado**: TERMINADA (2026-10-18). Todos los criterios cumplen y están validados: D1 en un esquema aislado y D2 en `motor_scoring_it.public`. V6 y V7 están aplicadas también en la base principal `motor_scoring` (ver la sección de HU12). Pendiente de autorización explícita: commit y push (no realizados).
* **Descripción breve**: Definir, validar, publicar, versionar y consultar las políticas de clasificación del riesgo crediticio (rangos de puntaje para las categorías `BAJO`, `MEDIO` y `ALTO`). En la Fase 1 la publicación y la consulta se ejecutan mediante casos de uso, sin endpoint HTTP; la exposición HTTP y la restricción por rol dependen de HU10.
* **Criterios de aceptación y estado**:
  * CA01 - Consultar la política vigente: **cumple** (caso de uso, sin endpoint HTTP).
  * CA02 - Crear una nueva versión: **cumple**.
  * CA03 - Exactamente una categoría para `BAJO`, `MEDIO` y `ALTO`: **cumple**.
  * CA04 a CA08 - Límites enteros entre -100 y 130, rangos válidos, sin solapamientos ni discontinuidades y cobertura completa: **cumplen**.
  * CA09 - Rechazar políticas incompletas o categorías duplicadas (redacción actualizada formalmente por María Alejandra, que reemplaza "incompletas o contradictorias"): **cumple**. Se rechaza la política si falta alguna de las categorías del modelo (`BAJO`, `MEDIO`, `ALTO`) o si una categoría aparece más de una vez, reportando todos los errores juntos. Por decisión aprobada, no se exige un orden fijo entre categorías.
  * CA10 - Publicar solo políticas válidas: **cumple**.
  * CA11 - Inmutabilidad de las políticas publicadas: **cumple** en el flujo de la aplicación (el puerto no ofrece modificación ni eliminación y las entidades JPA no generan `UPDATE`); no impide modificaciones mediante SQL directo.
  * CA12 - Conservación de versiones anteriores: **cumple**.
  * CA13 - Cada evaluación conserva la versión de política que utilizó: **cumple** (implementado en HU12). Validado el 2026-10-18 sobre PostgreSQL en un esquema aislado con V8 por `ClasificacionEvaluacionIT` (persistencia y recuperación de la versión; una política nueva no reclasifica). Sujeto a la promoción de V8.
  * CA14 - Fecha, hora y autor de publicación: **cumple** (validado el 2026-10-18 por D1 y D2). Fecha y hora cumplen. El autor (claim `sub` del JWT de HU10) se obtiene con `ActorActualPort` / `SecurityContextActorActualAdapter` y se persiste en `publicado_por` (migración `V7__add_autor_politica_clasificacion.sql`, con `CHECK ... NOT VALID` para conservar las versiones históricas sin autor). Pruebas sin base de datos: `SecurityContextActorActualAdapterTest`, `PublicarPoliticaClasificacionServiceTest`. D1 (`PoliticaClasificacionV7MigracionIT`) se ejecutó el 2026-10-18 en el esquema temporal `hu11_v7_historico_it`: **1 prueba, 0 fallos**, código de salida 0. Flyway aplicó V1–V6, la prueba insertó una versión histórica sin autor y Flyway aplicó V7; la versión histórica se conservó y las nuevas exigen autor; el esquema se eliminó al terminar. D2 (`PoliticaClasificacionRepositoryAdapterIT`, persistencia del autor por el adaptador en `motor_scoring_it.public`) se ejecutó el 2026-10-18 después de promover V8: **21 pruebas, 0 fallos** (ver la sección de HU12).
  * CA15 - Una publicación inválida o fallida no sustituye la política vigente: **cumple**.
  * CA16 - Publicación restringida al rol `ADMINISTRADOR_RIESGO`: **cumple** (desbloqueado por HU10). `SecurityConfig` exige `ADMINISTRADOR_RIESGO` para `POST /api/v1/politicas-clasificacion`, y la consulta de la vigente admite ambos roles; cubierto por `SeguridadIntegracionTest` y `PoliticaClasificacionControllerTest`.
* **Implementación**:
  * Módulo `scoring` (Package by Feature, Ports & Adapters).
  * Dominio sin dependencias de Spring ni JPA: enum `CategoriaRiesgo`, record `DefinicionRango`, `RangoClasificacion` y `PoliticaClasificacion` (inmutable; valida en conjunto cantidad de rangos, categorías únicas, límites, solapamientos, huecos y cobertura de -100 a 130, reportando todos los errores en `PoliticaClasificacionInvalidaException`). Excepciones `ConfiguracionClasificacionNoInicializadaException`, `PublicacionPoliticaConcurrenteException` y `PoliticaVigenteNoDisponibleException`.
  * Publicación: `PublicarPoliticaClasificacionUseCase` / `PublicarPoliticaClasificacionService` (`@Transactional`), puerto `PoliticaClasificacionRepositoryPort` (únicamente `publicar`). Fecha de creación y de publicación en un único instante.
  * Consulta (CA01): `ConsultarPoliticaVigenteUseCase` / `ConsultarPoliticaVigenteService` (`@Transactional(readOnly = true)`), con un puerto de salida independiente `PoliticaClasificacionConsultaPort` (`buscarVigente`, que retorna vacío sin política vigente). La ausencia de política vigente se comunica explícitamente con `PoliticaVigenteNoDisponibleException`.
  * `PoliticaClasificacionRepositoryAdapter` implementa ambos puertos. La publicación bloquea la fila singleton de configuración (`PESSIMISTIC_WRITE`) para serializar publicaciones concurrentes, y `UNIQUE(version)` actúa como protección adicional. La consulta lee la configuración sin bloqueo y los rangos ordenados por límite inferior (`RangoClasificacionJpaRepository.findByIdPoliticaOrderByLimiteInferior`).
  * Migración `V6__create_politica_clasificacion_tables.sql` (aprobada): tablas `politica_clasificacion`, `rango_clasificacion` y `configuracion_clasificacion_actual` (fila singleton), con restricciones `UNIQUE`, `CHECK` y claves foráneas. No se modificaron V1-V5. CA01 no requirió una nueva migración.
  * No se modificaron `pom.xml`, `.github/workflows/ci.yml`, `SecurityConfig`, `GlobalExceptionHandler`, `feature-list.json`, las migraciones V1-V6 ni otros módulos.
* **Pruebas**:
  * Unitarias: `PoliticaClasificacionTest` (27, incluidas las de CA09: categoría duplicada, categoría faltante y aceptación de una política válida sin un orden fijo entre categorías), `RangoClasificacionTest` (6), `PublicarPoliticaClasificacionServiceTest` (7), `PublicarPoliticaClasificacionRequestValidationTest` (4), `PoliticaClasificacionRepositoryPortTest` (1) y `ConsultarPoliticaVigenteServiceTest` (3: política vigente, ausencia explícita y configuración no inicializada).
  * Compilación y suite unitaria (`mvnw.cmd -B test`, sin `clean`, el 2026-10-16, tras los cambios de CA09): **312 pruebas ejecutadas, 0 fallidas, 0 errores, 0 omitidas**, `BUILD SUCCESS`, código de salida 0. (La ejecución previa a CA09, tras CA01, fue de 311 pruebas, también sin fallos.)
  * Integración contra PostgreSQL local real (`PoliticaClasificacionRepositoryAdapterIT`, perfil `it`, ejecutada aisladamente con `-Dtest=PoliticaClasificacionRepositoryAdapterIT -Dhu11.it.base-dedicada=motor_scoring_it` y `spring.datasource.url` apuntando a `motor_scoring_it`, el 2026-10-16, tras los cambios de CA09): **18 pruebas ejecutadas, 0 fallidas, 0 errores, 0 omitidas**, `BUILD SUCCESS`, código de salida 0. (La ejecución previa a CA09, tras CA01, fue de 16 pruebas, también sin fallos.)
    * Base de integración dedicada `motor_scoring_it` (PostgreSQL 18.6), distinta de `motor_scoring`. La prueba aborta antes de crear el contexto si la base no es la autorizada (nombre terminado en `_it`) o si las tablas de V1-V5 contienen datos.
    * Flyway validó las 6 migraciones existentes y no aplicó migraciones nuevas (esquema ya en la versión 6). Hibernate `ddl-auto=validate` no reportó incompatibilidades.
    * Cubre publicación, versionado, conservación de versiones anteriores, inmutabilidad, atomicidad ante fallos, serialización de publicaciones concurrentes, restricciones de V6 (incluido el rechazo de una categoría duplicada por `UNIQUE (id_politica, categoria)`) y, para CA01, cuatro casos nuevos: consulta de la política recién publicada con sus rangos, consulta tras publicar una nueva versión conservando la anterior, ausencia de política vigente y fila de configuración ausente.
    * Para CA09, dos casos nuevos a través del caso de uso: una política con una categoría duplicada y otra con una categoría faltante se rechazan con `PoliticaClasificacionInvalidaException` sin persistir versiones, políticas ni rangos nuevos, y la política vigente permanece intacta.
    * Cada prueba elimina únicamente los datos que crea; la limpieza hace fallar la prueba si no se completa.
  * Gherkin: `docs/quality/HU11-crear-publicar-politica-clasificacion.feature` (incluye los escenarios de consulta de CA01, el escenario de CA09 que documenta que no se exige un orden fijo entre categorías y los escenarios marcados `@pendiente-HU10`).
* **Observaciones**:
  * Pendientes (actualizado el 2026-10-18): ninguno de sus criterios. V9 (P2) está promovida, validada y aplicada en `motor_scoring_it.public` y en `motor_scoring`.
  * Las decisiones que quedaban pendientes para HU12 se resolvieron el 2026-10-17 (ver la sección HU12).
  * P2 (2026-10-17): la inmutabilidad de las políticas también se protege en la base de datos con la migración `V9__proteger_inmutabilidad_politica_clasificacion.sql` (2026-10-18): triggers que rechazan con SQLSTATE `55000` cualquier `UPDATE`, `DELETE` o `TRUNCATE` sobre `politica_clasificacion` y `rango_clasificacion`; publicar una versión nueva (INSERT) y cambiar la vigente (`configuracion_clasificacion_actual`) siguen permitidos. Sin efecto sobre el código de producción.
    * Validación en esquemas temporales de `motor_scoring_it`: `PoliticaClasificacionV9MigracionIT` (esquema `hu11_v9_it`), **9 pruebas, 0 fallos**; D2 con V9, **21 pruebas, 0 fallos**; `EvaluacionV8MigracionIT`, limitada a `target("8")` porque es la prueba de V8, **13 pruebas, 0 fallos**. Los tres esquemas se eliminaron solos.
    * D2 adaptada (opción 1 aprobada): se ejecuta en un esquema temporal propio que no debe existir antes (`hu11.it.esquema-dedicado`, coherente con `currentSchema` y `spring.flyway.schemas`); ya no borra políticas fila a fila (lo impide V9) y solo restaura la configuración vigente; un `@AfterAll` elimina el esquema completo con las mismas garantías que `EvaluacionV8MigracionIT` (bloqueo, mismo OID y marca de creación de Flyway). No se eliminó ninguna aserción.
    * Promovida el 2026-10-18 a `src/main/resources/db/migration` sin su cabecera de propuesta (sentencias SQL idénticas, SHA-256 de las sentencias `ec3519e1…386d`); propuesta original y SHA-256 en `build/respaldo-v9-promocion/`. Después de promoverla: suite sin base de datos real **521 pruebas, 0 fallos**; D2 con V9 desde `classpath:db/migration` **21 pruebas, 0 fallos**.
    * Aplicada el 2026-10-18 en `motor_scoring_it.public` con la API de Flyway (URL exacta, esquema `public`, solo `classpath:db/migration`, `cleanDisabled(true)`, `baselineOnMigrate(false)`, `target("9")`): `validate()` correcto; `info()` con V1–V8 aplicadas y V9 como única pendiente; `migrate()` ejecutó 1 migración, versión final 9. Inspección posterior: V1–V9 con éxito, los cuatro triggers de V9 presentes, conteos y configuración idénticos (tablas vacías, `id_politica` NULL) y ningún esquema `auth` en esa base (ni antes ni después).
    * Aplicada el 2026-10-18 en la base principal `motor_scoring` con el mismo procedimiento. Respaldo previo con `pg_dump`: `respaldos/2026-10-18-motor-scoring/motor_scoring-antes-v9-2026-10-18.dump`, SHA-256 `1765d854b0dec0e08dda24a0114e9ca6efc43bf1ae0f25389874a7751a4e14b0` (guardado junto al archivo), verificado con `pg_restore --list` (incluye `public` y `auth`). `validate()` correcto; `info()` con V1–V8 aplicadas y V9 como única pendiente; `migrate()` ejecutó 1 migración, versión final 9. Inspección posterior: V1–V9 con éxito, los cuatro triggers de V9 presentes, conteos de `public` idénticos (38, 5, 42, 3, 90; tablas de políticas vacías; configuración `id_politica` NULL) y huella de `auth` (filas e historial de Flyway) idéntica.
  * Advertencia no bloqueante: Hibernate Validator informa (HV000271) que `@Valid` sobre `List` en `PublicarPoliticaClasificacionRequest.rangos` está obsoleto; no se corrigió por estar fuera del alcance.
  * V6 crea tres tablas adicionales a las cinco listadas en `CLAUDE.md` §11; la migración fue aprobada explícitamente por María Alejandra.

## Sprint 2

### HU10 - Autorizar operaciones según el rol

* **Estado**: VERIFICADA en las tres APIs dentro del alcance actual de cada una (2026-10-17). Los endpoints de HU11, HU12 y HU13–HU15 se añadirán a cada matriz cuando se implementen. Pendiente de autorización explícita: commit y push (no realizados).
* **Descripción breve**: Cada API valida localmente el JWT RS256 emitido por `auth-api` con su clave pública (sin llamadas HTTP entre APIs) y aplica su propia matriz de permisos por rol en el backend. Roles: `ADMINISTRADOR_RIESGO` y `ANALISTA_CREDITO`.
* **Respaldo previo**: `respaldos/2026-10-17` (R1), verificado: parche de los 7 archivos modificados y TAR de los 37 sin seguimiento de `scoring-api` (manifiesto de 44 entradas, HEAD `cae6358`) y TAR de los 111 archivos de `auth-api`, ambos comprobados sobre copias extraídas.
* **scoring-api (este repositorio)**:
  * Validación JWT (`config/JwtConfig`, `JwtProperties`): solo RS256, emisor (`AUTH_JWT_ISSUER`), audiencia `scoring-api`, `exp` obligatorio con 60 s de tolerancia, `sub` UUID y `role` de texto. Solo los dos roles del modelo otorgan autoridades; un rol desconocido autentica sin permisos (403). El rol proviene únicamente del claim `role`.
  * `SecurityConfig`: matriz aprobada (`POST /solicitantes` y `POST /evaluaciones` para ANALISTA; `GET /solicitantes/documento/*` para ambos; variables de riesgo y reglas de scoring para ADMINISTRADOR), sin sesión HTTP, `denyAll` por defecto, `health` público sin detalles, Swagger público solo con el perfil `dev`, CORS con `Authorization` y `X-Trace-Id` expuesto.
  * `RespuestaSeguridadJson`: 401 `NO_AUTENTICADO` y 403 `ACCESO_DENEGADO` con `ApiError` y `traceId`.
  * Defecto corregido durante HU10: las respuestas 401/403 no incluían `traceId` porque `TraceIdFilter` se ejecutaba después de Spring Security; se agregó `@Order(Ordered.HIGHEST_PRECEDENCE)`.
  * Dependencias agregadas: `spring-boot-starter-security-oauth2-resource-server` y `spring-boot-starter-security-test` (pruebas).
  * Configuración: `AUTH_JWT_ISSUER` y `SCORING_JWT_PUBLIC_KEY_LOCATION` obligatorias (sin ellas no arranca); `application-dev.properties` habilita Swagger; `.gitignore` excluye `*.pem`, `*.key`, `*.p12`, `*.jks`, `keys/` y `secrets/`.
  * Documentación: `docs/security/matriz-permisos-scoring-api.md` (CA09) y sección de variables de entorno y clave pública en `README.md`.
  * Pruebas: `JwtConfigTest` (16) y `SeguridadIntegracionTest` (57, matriz completa con tokens RS256 reales firmados con claves efímeras). Las 4 pruebas de controlador y `contextLoads` se adaptaron para autenticarse con el rol correspondiente (sin eliminar aserciones; verificado que solo se agregaron líneas). Suite completa con `mvnw.cmd -B test`: **404 pruebas, 0 fallos, 0 errores**.
* **auth-api**: HU10 ya implementada (registro de usuarios solo para ADMINISTRADOR, `denyAll` por defecto, 401/403 con `ApiError`). Verificada sin modificar ningún archivo, ejecutando sus pruebas sobre una copia: **94 pruebas, 0 fallos**. Los 111 archivos siguen idénticos al respaldo R1.
* **evaluation-query-api**: esqueleto mínimo creado para HU10 (Spring Boot 4.1.1, misma validación JWT con audiencia `evaluation-query-api`, `SecurityConfig` con `denyAll` por defecto y `health` público, `TraceIdFilter`, README y matriz de permisos). Sin endpoints de negocio ni base de datos. Pruebas: **30 pruebas, 0 fallos**.
* **Pendientes**:
  * Defecto independiente (no corregido, fuera del alcance de HU10): `GlobalExceptionHandler` de `scoring-api` convierte un recurso inexistente (`NoResourceFoundException`) en HTTP 500 en vez de 404 mediante su manejador genérico de `Exception`.
  * `auth-api` no exige la presencia de `exp`, `sub` y `role` al validar tokens entrantes (usa el validador por defecto de Spring), a diferencia de `scoring-api` y `evaluation-query-api`. Riesgo bajo porque `auth-api` siempre los emite; no se modificó.
  * `auth-api` y `evaluation-query-api` no están bajo control de versiones Git.
  * Rotación de claves: las APIs consumidoras validan con una sola clave pública a la vez.
  * El frontend debe enviar `Authorization: Bearer <token>`; sin ello, todos los endpoints de negocio responden 401.
  * Agregar a las matrices los endpoints de HU13–HU15 cuando se implementen (los de HU11 y HU12 ya figuran en la matriz de scoring-api).

### HU12 - Clasificar una evaluación

* **Estado**: TERMINADA (2026-10-18). Todos los criterios están implementados y verificados; V8 está promovida y aplicada, junto con V7, en `motor_scoring_it.public` (D2) y, con V6, en la base principal `motor_scoring`. Pendiente de autorización explícita: commit y push (no realizados).
* **Decisiones aprobadas (2026-10-17)**: (1) sin política vigente, 409 `POLITICA_VIGENTE_NO_DISPONIBLE` sin persistir; (2) puntaje sin rango, 409 `PUNTAJE_SIN_CLASIFICACION` sin persistir; (3) se mantienen el 201 y los campos existentes, y se agregan `categoria`, `idPolitica` y `versionPolitica`; (4) excepción a `CLAUDE.md` §12 solo para persistir `score_total`, `categoria` e `id_politica` (CA05); (5) pruebas de integración de HU12 en esquemas temporales propios; (6) `GET` por id solo `ANALISTA_CREDITO`; (7) evaluaciones históricas con clasificación NULL, sin reclasificar; (8) triggers A, B y C en una propuesta revisable; (9) protección de los IT que usaban `DB_URL` (`ProteccionBaseIntegracion`). P1: V8 aprobada solo de forma condicional, sujeta a `EvaluacionV8MigracionIT`. P2: ver HU11. P3: plan por etapas.
* **Decisiones tomadas el 2026-10-18 (sin cambios de código)**:
  * A1: se mantiene `POLITICA_VIGENTE_NO_DISPONIBLE` con 404 en `GET /api/v1/politicas-clasificacion/vigente` (el recurso consultado no existe) y con 409 en `POST /api/v1/evaluaciones` (conflicto de negocio que impide registrar la evaluación).
  * A2: si falta la fila única de `configuracion_clasificacion_actual` (`ConfiguracionClasificacionNoInicializadaException`), la API responde 500 `INTERNAL_ERROR`: es una configuración inválida del servidor, no un error del cliente. V6 crea siempre esa fila.
  * A3: se mantiene el orden actual. La política vigente se lee una sola vez, dentro de la transacción y antes de persistir, después de evaluar las reglas; cualquier rechazo ocurre sin escribir nada.
  * Eliminación de evaluaciones: ninguna HU la requiere; no se agrega funcionalidad ni trigger para impedirla.
  * V8: los ajustes opcionales c (caso con varios detalles) y d (comprobación del `search_path`) no se aplican; la propuesta queda como está. Efecto documentado de su restricción `NOT VALID`: PostgreSQL también la comprueba en los `UPDATE`, de modo que las evaluaciones históricas no pueden actualizarse en ninguna columna (la aplicación no las actualiza).
* **Criterios** (actualizado el 2026-10-18):
  * CA01–CA12: **cumplen**. Las pruebas unitarias y de contrato cubren la aplicación; `ClasificacionEvaluacionFlujoTest` cubre el flujo sobre H2; `ClasificacionEvaluacionIT` cubre el flujo completo sobre PostgreSQL con V8 (CA02, CA04, CA05, CA06, CA09, CA10 y CA11); `EvaluacionV8MigracionIT` cubre las restricciones y los triggers A, B y C (CA03, CA06, CA07 y CA08) y la conservación de evaluaciones históricas sin clasificación.
  * Alcance de la validación: esquemas temporales y aislados de `motor_scoring_it` y, con D2, `motor_scoring_it.public`. V8 no está aplicada en `motor_scoring`.
* **Implementación**: módulo `evaluations` (dominio `ClasificacionEvaluacion`; puertos `ClasificacionRiesgoPort` y `EvaluacionConsultaPort`; servicios `CalcularScoreService` y `ConsultarEvaluacionService`; adaptadores `ClasificacionRiesgoAdapter` y `EvaluacionRepositoryAdapter`; `GET /api/v1/evaluaciones/{idEvaluacion}`). Migración `V8__add_clasificacion_evaluacion.sql` (columnas, restricciones y triggers A, B y C), promovida el 2026-10-18 a `src/main/resources/db/migration` desde la propuesta validada: se retiró solo su cabecera de propuesta y las 109 líneas de sentencias SQL son idénticas (SHA-256 de las sentencias `53738930…ac8065`). La propuesta original y su SHA-256 se conservan en `build/respaldo-v8-promocion/`. V8 no está aplicada en `motor_scoring_it.public` ni en `motor_scoring`. Gherkin: `docs/quality/HU12-clasificar-evaluacion.feature`.
* **Pruebas ejecutadas (2026-10-18)**: suite completa sin base de datos real (`mvnw.cmd -B test` con H2 en memoria forzado y Flyway desactivado; Surefire excluye las clases `*IT`): **521 pruebas, 0 fallos, 0 errores, 0 omitidas**, `BUILD SUCCESS`, código de salida 0. El log solo registra conexiones `jdbc:h2:mem:testdb`, ninguna a PostgreSQL y ninguna actividad de Flyway. Incluye `ClasificacionEvaluacionFlujoTest` (6 pruebas, flujo completo de HU12 sobre H2). Ejecución anterior: 515 pruebas, 0 fallos (2026-10-17).
* **Pruebas de integración ejecutadas (2026-10-18, PostgreSQL local `motor_scoring_it`, solo esquemas temporales)**:
  * `EvaluacionV8MigracionIT`, esquema `hu12_v8_it`: Flyway aplicó V1–V7 y luego la V8 propuesta. **13 pruebas, 0 fallos**, código de salida 0. La propia prueba eliminó el esquema tras comprobar que lo había creado (C3), sin error de limpieza. Se cumple así la condición de P1 para V8.
  * `ClasificacionEvaluacionIT`, esquema `hu12_clasificacion_it`: el primer intento falló al iniciar el contexto por un defecto de la prueba (un `@TempDir` estático no se inyecta antes de cargar el contexto con `@TestInstance(PER_CLASS)`), después de que Flyway aplicara V1–V8 y sin escribir datos. Corregido el defecto (el directorio de la clave se crea en el proveedor de la propiedad), la segunda ejecución reutilizó el esquema (Flyway validó y no migró nada): **7 pruebas, 0 fallos**, código de salida 0. El esquema se eliminó después con un programa de limpieza autorizado que exigió la marca de creación de Flyway; quedó confirmado que ya no existe.
  * `EvaluacionRepositoryAdapterIT`: no ejecutada (opcional; `ClasificacionEvaluacionIT` cubre el adaptador).
  * Logs en `build/pruebas-hu12/` (ignorado por Git).
* **Después de promover V8 (2026-10-18)**: suite completa sin base de datos real (H2 forzado, Flyway desactivado): **521 pruebas, 0 fallos**, código de salida 0, solo conexiones `jdbc:h2:mem:testdb`. A4 aplicada tras el respaldo `build/respaldo-a4-2026-10-18/` (manifiesto SHA-256): los comandos documentados ya no usan `-Dsurefire.failIfNoSpecifiedTests=false` ni la ubicación de la propuesta.
* **D2 sobre `motor_scoring_it.public` (2026-10-18)**: comprobaciones previas de solo lectura correctas (V1–V6 aplicadas, tablas de V1–V5 vacías, ninguna otra sesión). Primer intento: Flyway aplicó V7 y V8 en `public` con éxito, pero el contexto no arrancó porque la prueba, anterior a HU10, no configuraba la clave pública del JWT; no se escribieron datos. Corregida la prueba (solo se añadieron `scoring.jwt.emisor` y una clave pública efímera, con el patrón de `ClasificacionEvaluacionIT`), la segunda ejecución: **21 pruebas, 0 fallos**, código de salida 0, sin migraciones nuevas. Lectura posterior: V1–V8 con éxito, configuración restaurada (`id_politica` NULL), 0 filas residuales y solo el esquema `public`.
* **Aplicación en la base principal `motor_scoring` (2026-10-18, autorizada)**:
  * Lectura previa de solo lectura: Flyway en **V5** (V6 no estaba aplicada); datos reales en `public` (`solicitante` 38, `riesgo` 5, `regla_scoring` 42, `evaluacion` 3, `detalle_evaluacion` 90); esquema `auth` de `auth-api` en la misma base; ninguna otra sesión.
  * Respaldo previo completo con `pg_dump` 18.6 (formato personalizado): `respaldos/2026-10-18-motor-scoring/motor_scoring-antes-v6-v8-2026-10-18.dump`, SHA-256 `345aaef2d42d5affd6e6014d9ff295d3fb794ea52de14b182bbfa34ecb70736d` (guardado junto al archivo), verificado con `pg_restore --list` (incluye `public` y `auth` con sus datos).
  * Migración con la API de Flyway (programa en `build/migracion-motor-scoring/`, ignorado por Git): URL exacta `jdbc:postgresql://localhost:5432/motor_scoring`, esquema `public`, solo `classpath:db/migration`, `cleanDisabled(true)`, `baselineOnMigrate(false)`, `target("8")`. `validate()` correcto e `info()` con pendientes exactamente V6, V7 y V8; `migrate()` aplicó **3 migraciones** (V6, V7 y V8), versión final 8.
  * Lectura posterior: V1–V8 con éxito y checksums de V1–V5 sin cambios; conteos de `public` idénticos (38, 5, 42, 3 y 90); tablas de V6 creadas, sin políticas y con la configuración en `id_politica` NULL; huella de `auth` (filas e historial de Flyway) idéntica antes y después.
  * Consecuencias: las 3 evaluaciones existentes quedan como históricas, con clasificación NULL. Hasta que un `ADMINISTRADOR_RIESGO` publique una política, `POST /api/v1/evaluaciones` responde 409 `POLITICA_VIGENTE_NO_DISPONIBLE`.
* **Comprobación de arranque contra `motor_scoring` (2026-10-18, autorizada como "solo comprobación")**: con un emisor ficticio y una clave pública efímera de prueba (sin clave privada, borrada al terminar), sin perfil `dev`. Flyway validó 8 migraciones y no migró nada; Hibernate `validate` aceptó el esquema; la aplicación arrancó en 23 s en el puerto 8080; `/actuator/health` respondió 200 `UP`; `GET /api/v1/politicas-clasificacion/vigente` sin token respondió 401. Proceso detenido. No verifica la integración con los tokens reales de `auth-api`: hacerlo con `AUTH_JWT_ISSUER` y `SCORING_JWT_PUBLIC_KEY_LOCATION` reales antes del uso de punta a punta.
* **Pendientes**: comprobación con la configuración JWT real de `auth-api`. V9 (P2, ver HU11) ya está aplicada en `motor_scoring_it.public` y en `motor_scoring`.

### HU14 - Generar y consultar recomendaciones (saga)

* **Estado**: IMPLEMENTADA en `scoring-api` (rama `feature/hu14-saga-recomendaciones`) y en `evaluation-query-api` (rama `main` de su repositorio local), con commits locales y **sin push**. Verificada con pruebas automatizadas y con un flujo de extremo a extremo de las tres API en esquemas temporales de `motor_scoring_it`. **No desplegada**: `motor_scoring` sigue en V9, y el catálogo de recomendaciones incluido es un BORRADOR (P-HU14-01), con el que no se genera ninguna recomendación.
* **Decisiones aprobadas (2026-10-18)**: outbox `trabajo_recomendacion` en `public` con concesión y barrera `WHERE id_concesion = :token`; creación del trabajo con la concesión del primer intento; reintentos ante `40P01`/`55P03`; catálogo JSON versionado que solo genera con estado APROBADO; identidad S1. Valores de S1 y de la política de reintentos aprobados ("Valores sí, catálogo borrador"): 10 intentos (incluido el primero en línea), espera de 5 s duplicándose hasta 300 s con ±20 %, concesión de 30 s, en línea conexión 1 s y límite 2 s, reenvío conexión 2 s y límite 10 s, lotes de 20 cada 5 s.
* **Implementación en `scoring-api`**:
  * Migraciones `V10__indices_consultas_evaluacion.sql` y `V11__saga_recomendaciones.sql` (promovida sin cambios desde la propuesta; respaldo en `build/respaldo-v11-promocion`).
  * Dominio: `EstadoEvaluacion`, estado en `Evaluacion`, `PoliticaReintentos`. Aplicación: `CalcularScoreService.registrar` (evaluación PENDIENTE y trabajo en la misma transacción), `EvaluarSolicitanteService` (intento en línea después del commit), `ProcesoGeneracionRecomendaciones`, `ReenvioRecomendacionesService`. Infraestructura: `TrabajoRecomendacionAdapter`, `GeneracionRecomendacionesHttpAdapter`, `TokenServicioJwtFirmante`, `ReenvioRecomendacionesProgramado`. Todo lo de la saga es condicional a `scoring.saga.habilitada=true`. `EvaluacionResponse` incluye `estado`.
  * Corrección del 2026-10-18 detectada por el flujo de extremo a extremo: con la saga habilitada la aplicación no arrancaba porque `TokenServicioJwtFirmante` tenía dos constructores sin indicar cuál inyectar. Se marcó el público con `@Autowired` y se añadió `SagaRecomendacionesContextoTest`, que arranca el contexto con la saga habilitada (falla sin la corrección).
* **Implementación en `evaluation-query-api`**: módulo `recomendaciones` (dominio con el catálogo y la selección, servicios de generación idempotente y de consulta, adaptadores JDBC y de catálogo, controladores), esquema propio `consulta` con su migración V1 y triggers de inmutabilidad, cadena de seguridad de `/internal/**` para el token S1, 409 `EVALUACION_PENDIENTE` en HU13 y HU14, y en HU15 exclusión de las PENDIENTE, campo `estado` y enlace a las recomendaciones.
* **Pruebas ejecutadas (2026-10-18)**:
  * `scoring-api`, suite completa sin base de datos real: **555 pruebas, 0 fallos**.
  * `scoring-api`, `TrabajoRecomendacionAdapterIT` (3) y `TrabajoRecomendacionV11MigracionIT` (30) en esquemas temporales de `motor_scoring_it`: 0 fallos (ejecución anterior a esta corrección, que no les afecta).
  * `evaluation-query-api`, suite completa: **135 pruebas, 0 fallos**. `EsquemaScoringContratoIT` (V1–V11 de `scoring-api` y V1 propia, esquema temporal `eq_contrato_it`): **10 pruebas, 0 fallos**, esquema eliminado.
  * Flujo de extremo a extremo (programa en el directorio temporal de la sesión, no versionado): las tres API levantadas a la vez sobre los esquemas temporales `e2e_auth`, `e2e_scoring` y `e2e_consulta` de `motor_scoring_it`, con claves RSA efímeras, el primer administrador creado con `bootstrap-admin`, tokens reales de `auth-api`, un rol temporal de solo lectura para `evaluation-query-api` y un catálogo APROBADO solo de prueba. **32 comprobaciones, 0 fallos**: login de ambos roles; configuración y evaluación (COMPLETADA en la respuesta); el administrador consulta la evaluación (P-HU10-01); recomendaciones, explicación e historial con ambos roles; un token de usuario no autoriza la generación interna (401); Swagger con el perfil `dev`; con `evaluation-query-api` detenida la evaluación queda PENDIENTE y, al volver, el reenvío la completa; el rol de solo lectura no puede escribir en scoring ni modificar recomendaciones (SQLSTATE 42501). Esquemas, rol y claves eliminados y comprobado que no quedan restos.
* **Pendientes**:
  * Catálogo APROBADO por el negocio (P-HU14-01). P-HU14-02 a P-HU14-06 y P-HU14-08 están implementadas tal como se propusieron y requieren confirmación formal.
  * Despliegue en `motor_scoring`: procedimiento en `docs/operacion/despliegue-v10-v11.md` (**no ejecutado**). Con la saga deshabilitada, las evaluaciones nuevas quedan PENDIENTE.
  * Separación de roles de `scoring-api` (Flyway usa hoy el mismo rol que la aplicación, propietario de las tablas).
  * Commit de esta corrección, integración con `main` y push: cada uno requiere autorización expresa.

### Revisión de integración HU10–HU15 (2026-10-19)

* **Rama**: `revision/hu10-hu15-integracion` en los tres repositorios, creada desde la rama vigente de cada uno; cambios **sin confirmar**, pendientes de revisión y de autorización de commit.
* **Git**: restaurados los Javadoc y comentarios que eliminó sin justificación el PR #16 (commit `a2fce42`) en 26 archivos, incluida la licencia Apache de `mvnw` y `mvnw.cmd`; no se restauran los que quedaron obsoletos (RF16 en `SecurityConfig`, §12 en `Evaluacion`, cabeceras de IT que ya no usan `DB_URL`). `.claude/scheduled_tasks.lock` deja de versionarse; `.gitignore` excluye `.env`, respaldos y estado local de herramientas.
* **Defectos corregidos**: (1) Flyway aplicaba V10/V11 en cualquier arranque: ahora solo valida salvo `SCORING_FLYWAY_MIGRAR_AL_ARRANCAR=true` (`FlywayArranqueConfig`; igual en `evaluation-query-api`); (2) un fallo del intento en línea respondía 500 con la evaluación ya confirmada, lo que invitaba a duplicarla: ahora responde PENDIENTE; (3) `auth-api` no exigía `exp`, `sub` UUID ni `role`; (4) `auth-api` dejaba Swagger abierto sin el perfil `dev` y respondía 500.
* **Base de datos**: `scoring-api` y su Flyway usan un rol superusuario; preparados `docs/operacion/roles-scoring-api.sql` y los scripts de `evaluation-query-api` (no ejecutados en `motor_scoring`).
* **Pruebas**: `scoring-api` 559, `evaluation-query-api` 138, `auth-api` 99, 0 fallos; IT de PostgreSQL en esquemas temporales: 56 + 21 (`scoring-api`) y 10 (`evaluation-query-api`), 0 fallos; flujo de extremo a extremo v2: 70 comprobaciones, 0 fallos; `motor_scoring_it.public` sigue en V9.

### Cierre de pendientes y despliegue (2026-10-19)

* **Decisiones de la usuaria**: catálogo BORRADOR-1 aprobado sin cambios como versión 1 (P-HU14-01); definición de factor desfavorable confirmada (P-HU14-02); repositorios públicos `auth-api` y `evaluation-query-api`, creados por ella.
* **auth-api**: Flyway solo valida salvo `AUTH_FLYWAY_MIGRAR_AL_ARRANCAR=true`; script `roles-auth-api.sql` para entornos nuevos. En `motor_scoring` `auth-api` ya usaba roles no superusuario (`auth_migrator`, `auth_api`): se corrige lo afirmado en la revisión anterior.
* **Despliegue en `motor_scoring`** (autorizado): respaldo verificado por restauración, ensayo completo sobre la copia, V10 y V11 aplicadas, esquema `consulta` en V1, roles `scoring_propietario`, `scoring_app`, `consulta_propietario` y `consulta_app` sin contraseña, ajuste de `auth_api`. Datos intactos. Registro en `docs/operacion/despliegue-v10-v11.md`.
* **Pruebas**: `scoring-api` 559, `evaluation-query-api` 139, `auth-api` 102, 0 fallos; flujo de extremo a extremo v3 en `motor_scoring_it`: 91 comprobaciones, 0 fallos, con roles propietarios y de ejecución en las tres API, cookies en Chrome 155 y CA12 (p95 ≤ 35 ms con unas 1.400 evaluaciones).
* **Hallazgo de datos**: con las 42 reglas actuales de `motor_scoring` un solicitante suma 425 puntos, fuera de [-100, 130]: la evaluación se rechaza con 409 `PUNTAJE_SIN_CLASIFICACION`. No hay ninguna política publicada.
* **Pendiente de la usuaria**: contraseñas de los roles nuevos, configuración de las aplicaciones, claves de la saga y revisión de las reglas.
