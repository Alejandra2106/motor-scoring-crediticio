# language: es
Característica: Gestionar estado de variable de riesgo
  Como administrador de riesgo
  Quiero cambiar el estado de una variable de riesgo
  Para controlar su disponibilidad para las funcionalidades posteriores de cálculo del score crediticio

  Escenario: Inactivar una variable de riesgo activa
    Dado que existe una variable de riesgo registrada en estado ACTIVA
    Cuando cambio su estado a INACTIVA
    Entonces el sistema responde con estado 200
    Y la respuesta contiene el idRiesgo, el estadoAnterior ACTIVA y el estadoNuevo INACTIVA
    Y el nuevo estado queda persistido y puede consultarse posteriormente

  Escenario: Reactivar una variable de riesgo inactiva
    Dado que existe una variable de riesgo registrada en estado INACTIVA
    Cuando cambio su estado a ACTIVA
    Entonces el sistema responde con estado 200
    Y la respuesta contiene el idRiesgo, el estadoAnterior INACTIVA y el estadoNuevo ACTIVA
    Y el nuevo estado queda persistido y puede consultarse posteriormente

  Escenario: Rechazo por variable de riesgo inexistente
    Dado que envío un idRiesgo que no corresponde a ninguna variable registrada
    Cuando intento cambiar su estado
    Entonces el sistema responde con estado 404
    Y la respuesta indica que la variable de riesgo no fue encontrada
    Y no se realiza ningún cambio

  Escenario: Rechazo por estado fuera del conjunto permitido
    Dado que envío un estado que no es ACTIVA ni INACTIVA
    Cuando intento cambiar el estado de una variable de riesgo existente
    Entonces el sistema responde con estado 400
    Y no se realiza ningún cambio sobre la variable

  Escenario: Rechazo por estado ausente
    Dado que envío una solicitud de cambio de estado sin el campo estado
    Cuando intento cambiar el estado de una variable de riesgo existente
    Entonces el sistema responde con estado 400
    Y no se realiza ningún cambio sobre la variable

  Escenario: Preservación de la información de la variable al cambiar su estado
    Dado que existe una variable de riesgo registrada con su variable, descripción y fecha de creación
    Cuando cambio su estado
    Entonces el idRiesgo, la variable, la descripción y la fecha de creación permanecen sin modificaciones

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío datos inválidos para cambiar el estado de una variable de riesgo
    Cuando intento realizar el cambio
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de cambio de estado sin el encabezado X-Trace-Id
    Cuando cambio el estado de una variable de riesgo existente
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema
