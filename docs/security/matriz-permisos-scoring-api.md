# Matriz de permisos de scoring-api (HU10)

Aplicada en `src/main/java/com/crediticio/config/SecurityConfig.java` y verificada por
`src/test/java/com/crediticio/config/SeguridadIntegracionTest.java`.

## Autenticación

- Cada petición protegida exige un JWT de acceso emitido por `auth-api` en la cabecera
  `Authorization: Bearer <token>`. No hay sesión HTTP ni llamadas a `auth-api`.
- El token se valida localmente con la clave pública (`SCORING_JWT_PUBLIC_KEY_LOCATION`): firma
  RS256, emisor (`AUTH_JWT_ISSUER`), audiencia que incluya `scoring-api`, `exp` obligatorio con 60 s
  de tolerancia de reloj, `sub` con formato UUID y `role` de texto.
- El rol proviene únicamente del claim `role` del token validado. Los roles enviados en el cuerpo,
  en parámetros o en otras cabeceras se ignoran (CA08).

## Comportamiento implementado (matriz por endpoint)

Describe lo aplicado hoy en `SecurityConfig`. No todas las filas coinciden con los requisitos aprobados: ver
"Requisitos aprobados pendientes de implementación".

| Endpoint | ADMINISTRADOR_RIESGO | ANALISTA_CREDITO | Criterio |
| --- | --- | --- | --- |
| `POST /api/v1/solicitantes` | 403 | Permitido | CA03 |
| `GET /api/v1/solicitantes/documento/{numeroDocumento}` | Permitido | Permitido | CA04 |
| `POST /api/v1/evaluaciones` | 403 | Permitido | CA03 |
| `GET /api/v1/evaluaciones/{idEvaluacion}` | 403 | Permitido | Decisión 6 de HU12 (2026-10-17), superada por el requisito aprobado P-HU10-01: no cumple HU10 CA04 |
| `POST /api/v1/variables-riesgo` | Permitido | 403 | CA02 |
| `PATCH /api/v1/variables-riesgo/{idRiesgo}/estado` | Permitido | 403 | CA02 |
| `POST /api/v1/reglas-scoring` | Permitido | 403 | CA02 |
| `PATCH /api/v1/reglas-scoring/{idRegla}` | Permitido | 403 | CA02 |
| `GET /api/v1/politicas-clasificacion/vigente` | Permitido | Permitido | HU11 CA01 (decisión aprobada) |
| `POST /api/v1/politicas-clasificacion` | Permitido | 403 | CA02, HU11 CA16 |
| `GET /actuator/health` | Público, sin detalles | Público, sin detalles | Operación |
| Swagger (`/v3/api-docs/**`, `/swagger-ui/**`) | Solo con el perfil `dev` | Solo con el perfil `dev` | Operación |
| Cualquier otra ruta | 403 | 403 | Denegada por defecto |

HU12 añade `GET /api/v1/evaluaciones/{idEvaluacion}`. Su autorización está cubierta por pruebas sin
base de datos real: `SeguridadIntegracionTest` (cadena de seguridad real con casos de uso simulados) y
`EvaluacionControllerTest`. Sobre PostgreSQL lo validó `ClasificacionEvaluacionIT` el 2026-10-18 (200 para
`ANALISTA_CREDITO`, 403 para `ADMINISTRADOR_RIESGO` y 401 sin token).
Los rechazos funcionales de `POST /api/v1/evaluaciones` (409 `POLITICA_VIGENTE_NO_DISPONIBLE` y 409
`PUNTAJE_SIN_CLASIFICACION`) no son de autorización; se documentan en
`docs/quality/HU12-clasificar-evaluacion.feature`.

## Requisitos aprobados pendientes de implementación (2026-10-18)

Ninguna de estas filas está aplicada todavía; figurar aquí no significa que estén implementadas.

| Endpoint | ADMINISTRADOR_RIESGO | ANALISTA_CREDITO | Requisito |
| --- | --- | --- | --- |
| `GET /api/v1/evaluaciones/{idEvaluacion}` | Permitido | Permitido | HU10 CA04, P-HU10-01 |

- **P-HU10-01.** Ambos roles pueden consultar una evaluación. Solo afecta a la consulta: `POST
  /api/v1/evaluaciones` (proceso de scoring) sigue siendo solo de `ANALISTA_CREDITO` y la administración de
  variables, reglas y políticas, solo de `ADMINISTRADOR_RIESGO`. Cambiar `SecurityConfig`, sus pruebas, esta
  matriz y el Gherkin de HU12 requiere una autorización específica, todavía no concedida.
- **P-HU10-03.** Validación de los tokens conforme al contrato común, con comprobación de `exp`, `sub` y `role`.
  Requisito transversal; los detalles que el contrato no define (obligatoriedad de `iat`, `jti` y `kid`,
  tratamiento de `nbf` y composición de la audiencia) siguen pendientes. La sección "Autenticación" describe las
  comprobaciones actuales de esta API.

## Propuestas pendientes de decisión

- **P-HU10-04.** La duración nominal del token es de 15 minutos, pero se aceptan 60 s de tolerancia de reloj
  sobre `exp`, de modo que un token puede aceptarse hasta 16 minutos después de su emisión. Decidir si los 15
  minutos de CA10 son el límite absoluto de aceptación o si se admite una tolerancia técnica adicional.
- **P-HU10-02** (gestión de usuarios) corresponde a `auth-api` y no afecta a los endpoints de esta API.

## Endpoints propuestos no implementados

Ninguno en esta API. Las consultas de explicaciones, recomendaciones e historial (HU13–HU15) corresponden a
`evaluation-query-api`.

Requisitos: `docs/quality/HU10-autorizar-operaciones-segun-rol.feature` y
`docs/quality/HU12-clasificar-evaluacion.feature`.

## Respuestas de error

| Situación | Estado | `errorCode` |
| --- | --- | --- |
| Sin token, token mal formado, firma inválida, expirado, emisor o audiencia incorrectos, claims obligatorios ausentes | 401 | `NO_AUTENTICADO` |
| Token válido con un rol sin permiso para el endpoint, o con un rol desconocido | 403 | `ACCESO_DENEGADO` |

Ambas respuestas usan el formato `ApiError` e incluyen el `traceId` de la cabecera `X-Trace-Id`.

## Vigencia de los cambios de rol (CA10)

Los tokens de acceso duran 15 minutos y se validan sin estado; un cambio de rol o la
deshabilitación de un usuario se refleja como máximo en ese plazo.
