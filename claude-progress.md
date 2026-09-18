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

* **Estado**: PENDIENTE
* **Descripción breve**: Crear una nueva variable de riesgo utilizable en el scoring.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.

### HU04 - Editar variable de riesgo

* **Estado**: PENDIENTE
* **Descripción breve**: Editar una variable de riesgo existente.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.

### HU05 - Gestionar estado de variable de riesgo

* **Estado**: PENDIENTE
* **Descripción breve**: Gestionar (activar/inactivar) el estado de una variable de riesgo.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.

### HU06 - Crear regla de scoring

* **Estado**: PENDIENTE
* **Descripción breve**: Crear una nueva regla de scoring asociada a variables de riesgo.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.

### HU07 - Editar regla de scoring

* **Estado**: PENDIENTE
* **Descripción breve**: Editar una regla de scoring existente.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.

### HU08 - Calcular score crediticio

* **Estado**: PENDIENTE
* **Descripción breve**: Calcular el score crediticio de un solicitante aplicando las reglas de scoring vigentes.
* **Criterios de aceptación**: pendiente de documentar.
* **Implementación**: pendiente.
* **Pruebas**: pendiente.
* **Observaciones**: ninguna.
