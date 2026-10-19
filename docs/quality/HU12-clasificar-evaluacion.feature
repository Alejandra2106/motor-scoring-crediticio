# language: es
Característica: Clasificar una evaluación
  Como analista de crédito
  Quiero que cada evaluación obtenga una categoría de riesgo a partir de su puntaje total
  Para interpretar el resultado de forma consistente y trazable

  # Endpoints (HU10): POST /api/v1/evaluaciones (proceso de scoring) solo para ANALISTA_CREDITO (HU10 CA03).
  # GET /api/v1/evaluaciones/{idEvaluacion}: CA11 de esta historia exige restringir el acceso a los roles
  # autorizados, sin identificarlos; los roles los fija HU10 CA04. Requisito aprobado el 2026-10-18
  # (P-HU10-01 en HU10-autorizar-operaciones-segun-rol.feature): ambos roles pueden consultar una evaluación.
  # Implementado el 2026-10-18 (rama integracion/main-hu10-hu12); reemplaza la decisión 6 (2026-10-17), que lo
  # restringía a ANALISTA_CREDITO. La aprobación no amplía ninguna otra operación.
  # Categorías de riesgo del dominio: BAJO, MEDIO y ALTO. Los rangos de cada
  # categoría no son fijos: los define la política de clasificación publicada (HU11), que cubre el
  # intervalo de -100 a 130.
  # Contrato de HU07 ampliado de forma aditiva: la respuesta 201 conserva sus campos y agrega categoria,
  # idPolitica y versionPolitica; se agregan las respuestas 409 de los rechazos.
  # Validación (2026-10-18): pruebas unitarias; ClasificacionEvaluacionFlujoTest (H2); y, sobre PostgreSQL
  # con la migración V8, EvaluacionV8MigracionIT (restricciones y triggers), ClasificacionEvaluacionIT
  # (flujo completo) y D2 en motor_scoring_it.public.

  Escenario: Clasificación dentro del cálculo del score (CA01, CA02, CA03)
    Dado que existe una política de clasificación vigente
    Y un solicitante cuyo puntaje total pertenece al rango de la categoría MEDIO de esa política
    Cuando un usuario con rol ANALISTA_CREDITO calcula su score
    Entonces la respuesta es 201 con los campos existentes de la evaluación
    Y la categoría es MEDIO
    Y se indican el identificador y la versión exacta de la política utilizada

  Escenario: Rechazo sin política vigente (CA04)
    Dado que no existe una política de clasificación vigente
    Cuando un usuario con rol ANALISTA_CREDITO calcula el score de un solicitante
    Entonces la respuesta es 409 con el código POLITICA_VIGENTE_NO_DISPONIBLE
    Y no se registra ninguna evaluación

  Escenario: Rechazo de un puntaje sin clasificación (CA04)
    Dado que existe una política de clasificación vigente
    Y un solicitante cuyo puntaje total está fuera del intervalo de -100 a 130
    Cuando un usuario con rol ANALISTA_CREDITO calcula su score
    Entonces la respuesta es 409 con el código PUNTAJE_SIN_CLASIFICACION
    Y no se registra ninguna evaluación

  Escenario: Persistencia del puntaje, la categoría y la versión (CA05, CA07, CA08)
    Dado que existe una política de clasificación vigente
    Cuando un usuario con rol ANALISTA_CREDITO calcula el score de un solicitante
    Entonces la evaluación queda registrada con su puntaje total, su categoría y la referencia a la versión de la política
    Y el puntaje total registrado coincide con la suma de los puntajes de sus detalles

  Escenario: La clasificación original se conserva (CA06)
    Dado una evaluación registrada con una versión de la política
    Cuando se publica una nueva versión de la política
    Entonces la evaluación conserva su categoría y la referencia a la versión con la que se clasificó

  Escenario: Consulta de una evaluación por identificador (CA09)
    # Validado sobre PostgreSQL por ClasificacionEvaluacionIT (adaptador JPA y consulta SQL nativa de la versión).
    Dado una evaluación registrada
    Cuando un usuario con rol ANALISTA_CREDITO la consulta por su identificador
    Entonces obtiene su puntaje total, su categoría y la versión de política utilizada

  Escenario: Consulta de una evaluación inexistente (CA10)
    Cuando un usuario con rol ANALISTA_CREDITO consulta una evaluación con un identificador que no existe
    Entonces la respuesta es 404 con el código EVALUACION_NO_ENCONTRADA

  Escenario: El cálculo del score solo lo ejecuta ANALISTA_CREDITO (CA11, HU10 CA03)
    Cuando un usuario con rol ADMINISTRADOR_RIESGO calcula el score de un solicitante
    Entonces la respuesta es 403 con el código ACCESO_DENEGADO
    Y no se registra ninguna evaluación

  Escenario: Ambos roles pueden consultar una evaluación por identificador (CA11 con HU10 CA04, P-HU10-01)
    # Implementado el 2026-10-18; reemplaza el escenario de la decisión 6 (403 para ADMINISTRADOR_RIESGO).
    # Solo afecta a la consulta; no amplía el proceso de scoring ni la administración.
    Dado una evaluación registrada
    Cuando un usuario con rol ADMINISTRADOR_RIESGO o ANALISTA_CREDITO la consulta por su identificador
    Entonces obtiene su puntaje total, su categoría y la versión de política utilizada

  Escenario: La categoría no es una decisión de crédito (CA12)
    Cuando un usuario con rol ANALISTA_CREDITO calcula el score de un solicitante
    Entonces la respuesta no contiene ningún campo de aprobación ni de rechazo del crédito
