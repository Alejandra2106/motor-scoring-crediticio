# language: es
Característica: Autorizar operaciones según el rol
  Como responsable de la seguridad del motor de scoring
  Quiero controlar el acceso a cada operación según el rol del usuario autenticado
  Para proteger la información y evitar acciones no autorizadas

  # API responsable: transversal a auth-api, scoring-api y evaluation-query-api.
  # Este documento especifica requisitos; no declara el estado de implementación de ninguna API. El
  # cumplimiento de cada API se verifica con sus propias pruebas, nunca por la implementación de otra ni
  # por figurar en este documento o en una matriz de permisos.
  #
  # Criterios originales (texto sin cambios):
  #   CA01. Exigir autenticación mediante un JWT válido en todos los endpoints protegidos.
  #   CA02. Permitir a ADMINISTRADOR_RIESGO gestionar usuarios, variables de riesgo, reglas de scoring y
  #         políticas de clasificación.
  #   CA03. Permitir a ANALISTA_CREDITO registrar solicitantes y ejecutar evaluaciones.
  #   CA04. Permitir a ambos roles consultar solicitantes, evaluaciones, explicaciones, recomendaciones e
  #         historial.
  #   CA05. Aplicar la autorización en el backend de cada API, sin depender de las restricciones de la
  #         interfaz de usuario.
  #   CA06. Retornar HTTP 401 cuando el token no exista, sea inválido o esté expirado.
  #   CA07. Retornar HTTP 403 cuando el usuario esté autenticado, pero carezca de los permisos requeridos.
  #   CA08. Impedir que un usuario ejecute operaciones administrativas mediante modificación de solicitudes
  #         o manipulación de datos enviados.
  #   CA09. Documentar una matriz de permisos por rol y endpoint.
  #   CA10. Admitir una ventana máxima de 15 minutos para que los cambios de rol o la deshabilitación de un
  #         usuario se reflejen en tokens de acceso ya emitidos. No se exigirá revocación inmediata en esta
  #         versión.
  #
  # Aclaraciones (no cambian el alcance de los criterios):
  # - Roles: solo existen ADMINISTRADOR_RIESGO y ANALISTA_CREDITO (HU08 CA06). Un rol distinto en el token
  #   no otorga ningún permiso. El rol proviene únicamente del claim "role" del JWT validado.
  # - Requisitos comunes a las tres API: CA01, CA05, CA06, CA07, CA08, CA09 (una matriz por API), CA10 y el
  #   contrato JWT común (P-HU10-03), además del formato de error ApiError {errorCode, message, details,
  #   traceId} y la cabecera X-Trace-Id del contrato técnico común. Requisitos específicos: los permisos por
  #   operación de CA02, CA03 y CA04, según las operaciones que ofrece cada API (tabla siguiente).
  # - Se distinguen tres tipos de operación, sin ampliar permisos por inferencia:
  #   consultar una evaluación (CA04, ambos roles); ejecutar el proceso de scoring (CA03, solo
  #   ANALISTA_CREDITO); administrar variables, reglas y políticas (CA02, solo ADMINISTRADOR_RIESGO).
  # - Las operaciones internas entre servicios (generación de recomendaciones de HU14) no corresponden a
  #   ningún rol de usuario: exigen una identidad de servicio (diseño S1, preferido; valores pendientes de
  #   aprobación). Un token de usuario no las autoriza y un token de servicio no autoriza operaciones de
  #   usuario.
  #
  # Operaciones, roles y API (requisitos de CA02, CA03 y CA04; no describe lo implementado):
  #   | Operación                                   | API                  | ADMIN. | ANALISTA | Criterio            |
  #   | Registrar usuarios                          | auth-api             | Sí     | No       | CA02 (P-HU10-02)    |
  #   | Crear variable de riesgo y cambiar su estado| scoring-api          | Sí     | No       | CA02                |
  #   | Crear y editar reglas de scoring            | scoring-api          | Sí     | No       | CA02                |
  #   | Publicar política de clasificación          | scoring-api          | Sí     | No       | CA02                |
  #   | Consultar la política vigente               | scoring-api          | Sí     | Sí       | HU11 (decisión)     |
  #   | Registrar solicitante                       | scoring-api          | No     | Sí       | CA03                |
  #   | Ejecutar evaluación (proceso de scoring)    | scoring-api          | No     | Sí       | CA03                |
  #   | Consultar solicitante                       | scoring-api          | Sí     | Sí       | CA04                |
  #   | Consultar evaluación por identificador      | scoring-api          | Sí     | Sí       | CA04 (P-HU10-01)    |
  #   | Consultar explicación (HU13)                | evaluation-query-api | Sí     | Sí       | CA04                |
  #   | Consultar recomendaciones (HU14)            | evaluation-query-api | Sí     | Sí       | CA04                |
  #   | Consultar historial (HU15)                  | evaluation-query-api | Sí     | Sí       | CA04                |
  #   | Generar recomendaciones (interna, HU14)     | evaluation-query-api | No     | No       | Identidad de servicio |
  #   Login, renovación y cierre de sesión (auth-api) son públicos por diseño de HU09. Las rutas de HU13,
  #   HU14 y HU15 son propuestas pendientes de implementación.
  #
  # Decisiones aprobadas (2026-10-18):
  # - P-HU10-01 (requisito funcional aprobado; implementado en scoring-api el 2026-10-18). ADMINISTRADOR_RIESGO y
  #   ANALISTA_CREDITO pueden consultar evaluaciones, conforme a CA04. HU12 CA11 solo exige restringir el
  #   acceso a los roles autorizados, sin identificarlos, y no justifica restringir lo que CA04 garantiza.
  #   La aprobación solo afecta a la consulta: ejecutar el proceso de scoring sigue siendo exclusivo de
  #   ANALISTA_CREDITO (CA03) y la administración de variables, reglas y políticas, de ADMINISTRADOR_RIESGO
  #   (CA02). Reemplaza la decisión 6 de HU12, que restringía GET /api/v1/evaluaciones/{idEvaluacion} a
  #   ANALISTA_CREDITO.
  # - P-HU10-03 (requisito de seguridad aprobado; transversal). Las tres API validan los tokens conforme a un
  #   contrato común emitido por auth-api (HU09: RS256 con kid; iss; aud; sub; role; iat; exp; jti) y
  #   comprueban los claims obligatorios, incluidos la expiración (exp), el sujeto (sub) y el rol (role)
  #   cuando sean necesarios para autenticar y autorizar. Como todos los endpoints protegidos de las tres
  #   API se autorizan por rol, role es necesario en todos ellos. Esta aprobación no modifica código ni
  #   configuración. Detalles que el contrato actual no define (pendientes): si iat, jti y kid son
  #   obligatorios al validar; el tratamiento de nbf; y si la audiencia debe contener solo la API receptora o
  #   puede incluir otras (hoy auth-api emite la lista de las tres API).
  #
  # Decisiones pendientes (no se agregan como requisitos aprobados):
  # - P-HU10-02. CA02 dice "gestionar usuarios", pero el requisito no permite determinar qué operaciones
  #   comprende. Opciones que requieren una decisión funcional: (a) registrar usuarios (única operación hoy
  #   especificada, HU08); (b) deshabilitar usuarios; (c) reactivar usuarios; (d) modificar el rol de un
  #   usuario. CA10 menciona cambios de rol y deshabilitaciones, pero ninguna historia los especifica como
  #   operaciones.
  # - P-HU10-04. CA10 fija una ventana máxima de 15 minutos. La duración nominal del token de acceso es de
  #   15 minutos (exp), pero las tres API aceptan 60 s de tolerancia de reloj sobre exp, de modo que un token
  #   puede aceptarse hasta 16 minutos después de su emisión. Propuesta para decidir:
  #   (a) los 15 minutos son el límite absoluto de aceptación: la vigencia nominal más la tolerancia no debe
  #       superarlos (por ejemplo, vigencia de 14 minutos con 60 s de tolerancia);
  #   (b) los 15 minutos son la vigencia nominal y se admite expresamente una tolerancia técnica adicional,
  #       cuyo máximo debe fijarse (hoy 60 s).

  @transversal
  Esquema del escenario: Petición sin token o con un token inválido (CA01, CA06)
    Cuando se llama a un endpoint protegido de <api> <condicion>
    Entonces la respuesta es 401 con el código NO_AUTENTICADO, en formato ApiError y con su traceId

    Ejemplos:
      | api                  | condicion                                     |
      | auth-api             | sin cabecera Authorization                    |
      | auth-api             | con un token expirado                         |
      | scoring-api          | sin cabecera Authorization                    |
      | scoring-api          | con un token firmado con otra clave           |
      | scoring-api          | con un token expirado                         |
      | evaluation-query-api | sin cabecera Authorization                    |
      | evaluation-query-api | con un token mal formado                      |
      | evaluation-query-api | con un token cuya audiencia no incluye la API |

  @transversal
  Esquema del escenario: Claims obligatorios del contrato común (CA01, CA06, P-HU10-03)
    Cuando se llama a un endpoint protegido de <api> con un token firmado correctamente pero <defecto>
    Entonces la respuesta es 401 con el código NO_AUTENTICADO

    Ejemplos:
      | api                  | defecto      |
      | auth-api             | sin exp      |
      | auth-api             | sin sub      |
      | auth-api             | sin role     |
      | scoring-api          | sin exp      |
      | scoring-api          | sin sub      |
      | scoring-api          | sin role     |
      | evaluation-query-api | sin exp      |
      | evaluation-query-api | sin sub      |
      | evaluation-query-api | sin role     |

  @transversal
  Esquema del escenario: Usuario autenticado sin permiso para la operación (CA05, CA07)
    Dado un usuario autenticado con el rol <rol>
    Cuando llama a <operacion> en <api>
    Entonces la respuesta es 403 con el código ACCESO_DENEGADO, en formato ApiError y con su traceId

    Ejemplos:
      | rol                  | operacion                               | api         |
      | ANALISTA_CREDITO     | registrar un usuario                    | auth-api    |
      | ANALISTA_CREDITO     | crear una regla de scoring              | scoring-api |
      | ANALISTA_CREDITO     | publicar una política de clasificación  | scoring-api |
      | ADMINISTRADOR_RIESGO | registrar un solicitante                | scoring-api |
      | ADMINISTRADOR_RIESGO | ejecutar una evaluación                 | scoring-api |

  @scoring-api @auth-api
  Esquema del escenario: Operaciones de administración reservadas al administrador (CA02)
    Dado un usuario autenticado con el rol ADMINISTRADOR_RIESGO
    Cuando llama a <operacion> en <api>
    Entonces la operación se autoriza

    Ejemplos:
      | operacion                                   | api         |
      | registrar un usuario                        | auth-api    |
      | crear una variable de riesgo                | scoring-api |
      | cambiar el estado de una variable de riesgo | scoring-api |
      | crear una regla de scoring                  | scoring-api |
      | editar una regla de scoring                 | scoring-api |
      | publicar una política de clasificación      | scoring-api |

  @scoring-api
  Esquema del escenario: Operaciones del analista de crédito (CA03)
    Dado un usuario autenticado con el rol ANALISTA_CREDITO
    Cuando llama a <operacion> en scoring-api
    Entonces la operación se autoriza

    Ejemplos:
      | operacion                                    |
      | registrar un solicitante                     |
      | ejecutar una evaluación (proceso de scoring) |

  @scoring-api @evaluation-query-api
  Esquema del escenario: Consultas permitidas a ambos roles (CA04, P-HU10-01)
    # Requisito aprobado. Implementado en scoring-api (consulta de solicitantes y evaluaciones) y en
    # evaluation-query-api (HU13 y HU15); las recomendaciones (HU14) dependen de su implementación.
    Dado un usuario autenticado con el rol <rol>
    Cuando realiza la consulta de <consulta> en <api>
    Entonces la consulta se autoriza

    Ejemplos:
      | rol                  | consulta                     | api                  |
      | ADMINISTRADOR_RIESGO | un solicitante               | scoring-api          |
      | ANALISTA_CREDITO     | un solicitante               | scoring-api          |
      | ADMINISTRADOR_RIESGO | una evaluación               | scoring-api          |
      | ANALISTA_CREDITO     | una evaluación               | scoring-api          |
      | ADMINISTRADOR_RIESGO | la explicación (HU13)        | evaluation-query-api |
      | ANALISTA_CREDITO     | la explicación (HU13)        | evaluation-query-api |
      | ADMINISTRADOR_RIESGO | las recomendaciones (HU14)   | evaluation-query-api |
      | ANALISTA_CREDITO     | las recomendaciones (HU14)   | evaluation-query-api |
      | ADMINISTRADOR_RIESGO | el historial (HU15)          | evaluation-query-api |
      | ANALISTA_CREDITO     | el historial (HU15)          | evaluation-query-api |

  @transversal
  Escenario: El rol solo proviene del token validado (CA05, CA08)
    Dado un usuario autenticado con el rol ANALISTA_CREDITO
    Cuando envía una operación administrativa indicando el rol ADMINISTRADOR_RIESGO en el cuerpo, en un parámetro o en una cabecera distinta de Authorization
    Entonces la respuesta es 403 con el código ACCESO_DENEGADO
    Y no se ejecuta ninguna modificación

  @transversal
  Escenario: Un rol desconocido no otorga permisos (CA05, CA08)
    Dado un token válido cuyo claim role no es ADMINISTRADOR_RIESGO ni ANALISTA_CREDITO
    Cuando llama a cualquier endpoint de negocio de cualquiera de las tres API
    Entonces la respuesta es 403 con el código ACCESO_DENEGADO

  @transversal
  Escenario: Matriz de permisos por API (CA09)
    Entonces cada API publica una matriz de permisos por rol y endpoint
    Y la matriz distingue el comportamiento implementado, los requisitos aprobados pendientes de implementación, las propuestas pendientes y los endpoints propuestos no implementados

  @transversal
  Escenario: Ventana máxima para cambios de rol o deshabilitación (CA10)
    # La relación entre la ventana de 15 minutos y la tolerancia de reloj está pendiente (P-HU10-04).
    Dado un token de acceso emitido antes de un cambio de rol o de la deshabilitación del usuario
    Cuando ha vencido su exp, fijado en 15 minutos desde la emisión
    Entonces ninguna de las tres API acepta ese token
    Y no se exige revocación inmediata en esta versión
