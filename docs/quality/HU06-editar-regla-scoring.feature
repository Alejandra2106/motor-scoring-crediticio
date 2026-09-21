# language: es
Característica: Editar regla de scoring
  Como administrador de riesgo
  Quiero editar una regla de scoring existente
  Para mantener actualizado el criterio utilizado posteriormente para calcular el puntaje crediticio

  Escenario: Edición exitosa del operador, el valorCondicion y el puntaje
    Dado que existe una regla de scoring registrada sobre una variable de riesgo ACTIVA
    Cuando edito su operador, valorCondicion y puntaje con valores válidos
    Entonces el sistema responde con estado 200
    Y la respuesta contiene el idRegla, el idRiesgo, el operador, el valorCondicion y el puntaje actualizados
    Y el idRegla, el idRiesgo, el estado y la fecha de creación permanecen sin modificaciones

  Escenario: Rechazo por regla de scoring inexistente
    Dado que envío un idRegla que no corresponde a ninguna regla registrada
    Cuando intento editarla
    Entonces el sistema responde con estado 404
    Y no se realiza ninguna modificación

  Escenario: Rechazo por variable de riesgo inactiva
    Dado que existe una regla de scoring asociada a una variable de riesgo en estado INACTIVA
    Cuando intento editar esa regla
    Entonces el sistema responde con estado 409
    Y no se realiza ninguna modificación

  Escenario: Rechazo por operador incompatible con una variable categórica
    Dado que existe una regla de scoring sobre la variable HISTORIAL_CREDITICIO
    Cuando intento editarla con un operador distinto de "="
    Entonces el sistema responde con estado 400
    Y no se realiza ninguna modificación

  Escenario: Rechazo por valorCondicion no numérico en una variable numérica
    Dado que existe una regla de scoring sobre una variable numérica
    Cuando intento editarla con un valorCondicion no numérico
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por valorCondicion categórico fuera del catálogo permitido
    Dado que existe una regla de scoring sobre la variable HISTORIAL_CREDITICIO
    Cuando intento editarla con un valorCondicion fuera de BUENO, REGULAR o MALO
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por puntaje con notación decimal
    Dado que existe una regla de scoring registrada
    Cuando intento editarla con un puntaje decimal
    Entonces el sistema responde con estado 400
    Y no se realiza ninguna modificación

  Escenario: Rechazo por puntaje fuera del rango permitido
    Dado que existe una regla de scoring registrada
    Cuando intento editarla con un puntaje fuera de -100 a 100
    Entonces el sistema responde con estado 400

  Escenario: Rechazo por duplicar la combinación de otra regla existente
    Dado que existen dos reglas de scoring distintas sobre la misma variable de riesgo
    Cuando edito una de ellas para que su operador y valorCondicion coincidan con los de la otra
    Entonces el sistema responde con estado 409
    Y no se realiza ninguna modificación

  Escenario: Permitir editar una regla conservando su propia combinación
    Dado que existe una regla de scoring registrada
    Cuando la edito sin cambiar su operador ni su valorCondicion
    Entonces el sistema responde con estado 200

  Escenario: Preservación del idRiesgo y del estado al editar
    Dado que existe una regla de scoring registrada
    Cuando edito su operador, valorCondicion y puntaje
    Entonces el idRiesgo y el estado de la regla permanecen sin modificaciones

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío datos inválidos para editar una regla de scoring
    Cuando intento realizar la edición
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de edición sin el encabezado X-Trace-Id
    Cuando edito una regla de scoring existente
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema
