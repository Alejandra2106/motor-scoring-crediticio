# language: es
Característica: Crear regla de scoring
  Como administrador de riesgo
  Quiero crear una regla de scoring asociada a una variable de riesgo activa
  Para establecer una condición y un puntaje que puedan ser utilizados en el cálculo del score crediticio

  Escenario: Creación exitosa de una regla sobre una variable numérica
    Dado que existe una variable de riesgo numérica registrada en estado ACTIVA
    Cuando creo una regla de scoring con un operador y un valorCondicion numérico válidos y un puntaje dentro del rango permitido
    Entonces el sistema responde con estado 201
    Y la respuesta contiene el idRegla, el idRiesgo, el operador, el valorCondicion y el puntaje
    Y la regla queda persistida en estado ACTIVA

  Escenario: Creación exitosa de una regla sobre la variable categórica Historial crediticio
    Dado que existe la variable de riesgo HISTORIAL_CREDITICIO registrada en estado ACTIVA
    Cuando creo una regla de scoring con operador "=" y valorCondicion "BUENO"
    Entonces el sistema responde con estado 201

  Escenario: Rechazo por variable de riesgo inexistente
    Dado que envío un idRiesgo que no corresponde a ninguna variable registrada
    Cuando intento crear una regla de scoring
    Entonces el sistema responde con estado 404
    Y no se persiste ninguna regla

  Escenario: Rechazo por variable de riesgo inactiva
    Dado que existe una variable de riesgo registrada en estado INACTIVA
    Cuando intento crear una regla de scoring asociada a esa variable
    Entonces el sistema responde con estado 409
    Y no se persiste ninguna regla

  Escenario: Rechazo por operador incompatible con una variable categórica
    Dado que existe la variable de riesgo HISTORIAL_CREDITICIO registrada en estado ACTIVA
    Cuando intento crear una regla de scoring con operador ">=" sobre esa variable
    Entonces el sistema responde con estado 400
    Y no se persiste ninguna regla

  Escenario: Rechazo por valorCondicion no numérico en una variable numérica
    Dado que existe una variable de riesgo numérica registrada en estado ACTIVA
    Cuando intento crear una regla de scoring con valorCondicion "ABC"
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por valorCondicion decimal en NUMERO_MORAS
    Dado que existe la variable de riesgo NUMERO_MORAS registrada en estado ACTIVA
    Cuando intento crear una regla de scoring con valorCondicion "2.5"
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por valorCondicion categórico fuera del catálogo permitido
    Dado que existe la variable de riesgo HISTORIAL_CREDITICIO registrada en estado ACTIVA
    Cuando intento crear una regla de scoring con valorCondicion "EXCELENTE"
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por puntaje fuera del rango permitido
    Dado que existe una variable de riesgo numérica registrada en estado ACTIVA
    Cuando intento crear una regla de scoring con puntaje 101
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por regla duplicada
    Dado que ya existe una regla de scoring con un idRiesgo, operador y valorCondicion específicos
    Cuando intento crear otra regla con exactamente la misma combinación
    Entonces el sistema responde con estado 409
    Y no se persiste una segunda regla

  Escenario: Rechazo por datos obligatorios ausentes
    Dado que envío una solicitud de creación sin alguno de los campos idRiesgo, operador, valorCondicion o puntaje
    Cuando intento crear la regla de scoring
    Entonces el sistema responde con estado 400

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío datos inválidos para crear una regla de scoring
    Cuando intento realizar la creación
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de creación sin el encabezado X-Trace-Id
    Cuando creo una regla de scoring
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema
