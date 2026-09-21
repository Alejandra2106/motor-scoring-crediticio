# language: es
Característica: Crear variable de riesgo
  Como administrador de riesgo
  Quiero crear una nueva variable de riesgo a partir de un conjunto cerrado de variables
  Para que quede disponible para la evaluación crediticia

  Escenario: Creación exitosa de una variable de riesgo válida
    Dado que envío una variable del conjunto permitido con una descripción válida
    Cuando registro la variable de riesgo
    Entonces el sistema responde con estado 201
    Y la respuesta contiene el idVariableRiesgo, la variable, el tipo, la descripcion y el estado

  Escenario: Generación automática del identificador
    Dado que envío una variable del conjunto permitido con una descripción válida
    Cuando registro la variable de riesgo
    Entonces el idVariableRiesgo generado es único y no fue enviado por el cliente

  Escenario: Estado inicial ACTIVA
    Dado que envío una variable del conjunto permitido con una descripción válida
    Cuando registro la variable de riesgo
    Entonces el estado de la variable creada es ACTIVA

  Escenario: Persistencia exitosa en PostgreSQL
    Dado que envío una variable del conjunto permitido con una descripción válida
    Cuando registro la variable de riesgo
    Entonces la variable queda persistida y puede consultarse posteriormente con los mismos datos

  Escenario: Rechazo por variable duplicada
    Dado que ya existe una variable de riesgo registrada con un nombre de variable
    Cuando intento registrar una nueva variable de riesgo con el mismo nombre
    Entonces el sistema responde con estado 409
    Y la respuesta indica que la variable de riesgo ya se encuentra registrada
    Y no se crea un nuevo registro para esa variable

  Escenario: Rechazo por variable fuera del conjunto permitido
    Dado que envío un valor de variable que no pertenece a INGRESOS_MENSUALES, NIVEL_ENDEUDAMIENTO, NUMERO_MORAS, HISTORIAL_CREDITICIO ni ANTIGUEDAD_LABORAL
    Cuando intento registrar la variable de riesgo
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo variable es inconsistente

  Escenario: Rechazo por descripción menor al mínimo permitido
    Dado que envío una variable del conjunto permitido con una descripción de menos de 10 caracteres
    Cuando intento registrar la variable de riesgo
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo descripcion es inconsistente

  Escenario: Rechazo por descripción mayor al máximo permitido
    Dado que envío una variable del conjunto permitido con una descripción de más de 255 caracteres
    Cuando intento registrar la variable de riesgo
    Entonces el sistema responde con estado 400
    Y la respuesta indica que el campo descripcion es inconsistente

  Escenario: Rechazo por campos obligatorios ausentes
    Dado que envío una solicitud de creación sin el campo variable o sin el campo descripcion
    Cuando intento registrar la variable de riesgo
    Entonces el sistema responde con estado 400
    Y no se crea ninguna variable de riesgo

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío datos inválidos para crear una variable de riesgo
    Cuando intento realizar el registro
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de creación sin el encabezado X-Trace-Id
    Cuando registro una variable de riesgo válida
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema
