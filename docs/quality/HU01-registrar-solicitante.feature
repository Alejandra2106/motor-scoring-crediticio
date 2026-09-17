# language: es
Característica: Registrar solicitante
  Como analista de crédito
  Quiero registrar un solicitante con sus datos personales y financieros
  Para disponer de la información necesaria para realizar una evaluación de riesgo

  Escenario: Registro exitoso de un solicitante con datos válidos
    Dado que envío los datos completos y válidos de un solicitante nuevo
    Cuando registro al solicitante
    Entonces el sistema responde con estado 201 Created
    Y la respuesta contiene el identificador generado, el nombre completo, el número de documento y la fecha de registro
    Y los datos financieros y el historial crediticio del solicitante quedan persistidos correctamente

  Escenario: Rechazo por nombre completo inválido
    Dado que envío los datos de un solicitante con un nombre completo de menos de 3 caracteres o compuesto únicamente por espacios
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo nombreCompleto es inconsistente

  Escenario: Rechazo por número de documento inválido
    Dado que envío los datos de un solicitante con un número de documento que no tiene entre 6 y 15 dígitos
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo numeroDocumento es inconsistente

  Escenario: Rechazo por ingresos mensuales inválidos
    Dado que envío los datos de un solicitante con ingresos mensuales negativos
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo ingresosMensuales es inconsistente

  Escenario: Rechazo por deudas mensuales inválidas
    Dado que envío los datos de un solicitante con deudas mensuales negativas
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo deudasMensuales es inconsistente

  Escenario: Rechazo por número de moras inválido
    Dado que envío los datos de un solicitante con un número de moras negativo
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo numeroMoras es inconsistente

  Escenario: Rechazo por historial crediticio inválido
    Dado que envío los datos de un solicitante con un historial crediticio distinto de BUENO, REGULAR o MALO
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo historialCrediticio es inconsistente

  Escenario: Rechazo por antigüedad laboral inválida
    Dado que envío los datos de un solicitante con antigüedad laboral negativa
    Cuando registro al solicitante
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo antiguedadLaboral es inconsistente

  Escenario: Rechazo por número de documento duplicado
    Dado que ya existe un solicitante registrado con un número de documento
    Cuando registro un nuevo solicitante con el mismo número de documento
    Entonces el sistema responde con estado 409
    Y la respuesta indica que el número de documento ya se encuentra registrado
    Y no se crea un nuevo registro para ese número de documento

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío datos inválidos para registrar un solicitante
    Cuando registro al solicitante
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de registro sin el encabezado X-Trace-Id
    Cuando registro al solicitante
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema

  Escenario: Reutilización de un X-Trace-Id válido
    Dado que envío una solicitud de registro con un encabezado X-Trace-Id que es un UUID válido
    Cuando registro al solicitante
    Entonces la respuesta incluye el mismo valor de X-Trace-Id en el encabezado de respuesta
