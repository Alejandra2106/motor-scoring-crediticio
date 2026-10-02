# Política de versionado y retiro del contrato OpenAPI

Este documento define la política de compatibilidad y retiro exigida para el contrato OpenAPI
del backend (CODEF@CTORY, Equipo Avanzado, §3.3 y §3.1 de los Lineamientos Integrados).

## Relación entre `info.version` y `/api/v1`

Son dos versionados independientes y no deben confundirse:

* **`/api/v1`** es el versionado de la API REST a nivel de ruta (contrato de transporte).
* **`info.version`** (declarado en `OpenApiConfig`, actualmente `1.0.0`) es la versión propia
  del contrato OpenAPI/Swagger como artefacto documental.

Un cambio de `info.version` no implica necesariamente un cambio de `/api/v1`, y viceversa.

## Regla de versionado

* **Cambios compatibles** (agregar un endpoint nuevo, agregar un campo opcional a una
  respuesta, agregar un nuevo valor a un enum de forma aditiva, etc.) **no requieren** incrementar
  la versión mayor de `info.version`.
* **Cambios incompatibles** (eliminar o renombrar un campo, cambiar el tipo de un campo, eliminar
  un endpoint, cambiar el significado de un código de respuesta existente, etc.) **incrementan**
  la versión mayor de `info.version`.

## Retiro de endpoints

* Un endpoint que vaya a retirarse debe marcarse explícitamente como `deprecated` en su
  documentación OpenAPI (anotación `@Operation(deprecated = true)` o equivalente) antes de su
  eliminación.
* El endpoint deprecado debe documentarse indicando, cuando exista, el endpoint que lo
  reemplaza.
* La eliminación efectiva del endpoint deprecado se trata como un cambio incompatible y sigue
  la regla de versionado anterior.

## Alcance de este documento

Esta política aplica al contrato generado dinámicamente por springdoc-openapi a partir del
código (`/v3/api-docs`, `/swagger-ui.html`). No se mantiene un archivo OpenAPI estático adicional:
el contrato dinámico es la única fuente de verdad, siempre sincronizada con el código.
