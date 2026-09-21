# language: es
Característica: Consultar información del solicitante
  Como analista de crédito
  Quiero consultar la información registrada de un solicitante por su número de documento
  Para disponer de sus datos antes de realizar una evaluación de riesgo

  Escenario: Consulta exitosa de un solicitante existente
    Dado que existe un solicitante registrado con un número de documento conocido
    Cuando consulto al solicitante por ese número de documento
    Entonces el sistema responde con estado 200
    Y la respuesta contiene el idSolicitante, el nombreCompleto, el numeroDocumento, los ingresosMensuales, las deudasMensuales, el numeroMoras, el historialCrediticio, la antiguedadLaboral y la fechaRegistro

  Escenario: La consulta no modifica los datos almacenados del solicitante
    Dado que existe un solicitante registrado con un número de documento conocido
    Cuando consulto al solicitante por ese número de documento
    Entonces los datos persistidos del solicitante permanecen sin modificaciones

  Escenario: Rechazo por solicitante inexistente
    Dado que envío un número de documento que no corresponde a ningún solicitante registrado
    Cuando intento consultarlo
    Entonces el sistema responde con estado 404
    Y la respuesta indica que no se encontró un solicitante con ese documento

  Escenario: Rechazo por número de documento con formato inválido
    Dado que envío un número de documento que no tiene entre 6 y 15 dígitos
    Cuando intento consultar al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo numeroDocumento es inconsistente

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío un número de documento inválido para consultar un solicitante
    Cuando intento realizar la consulta
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de consulta sin el encabezado X-Trace-Id
    Cuando consulto a un solicitante existente
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema
