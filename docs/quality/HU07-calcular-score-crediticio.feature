# language: es
Característica: Calcular score crediticio
  Como analista de crédito
  Quiero calcular el score crediticio de un solicitante
  Para obtener una medida objetiva de su riesgo crediticio

  Escenario: Identificación exitosa del solicitante por idSolicitante
    Dado que existe un solicitante registrado con un idSolicitante conocido
    Cuando calculo su score indicando únicamente idSolicitante
    Entonces el sistema responde con estado 201
    Y la respuesta contiene el idEvaluacion, el idSolicitante, la fechaEvaluacion y el scoreTotal

  Escenario: Identificación exitosa del solicitante por numeroDocumento
    Dado que existe un solicitante registrado con un numeroDocumento conocido
    Cuando calculo su score indicando únicamente numeroDocumento
    Entonces el sistema responde con estado 201

  Escenario: Rechazo por solicitante inexistente
    Dado que envío un idSolicitante o un numeroDocumento que no corresponden a ningún solicitante registrado
    Cuando intento calcular su score
    Entonces el sistema responde con estado 404
    Y no se crea ninguna evaluación

  Escenario: Rechazo cuando se envían ambos identificadores
    Dado que envío tanto idSolicitante como numeroDocumento en la misma solicitud
    Cuando intento calcular el score
    Entonces el sistema responde con estado 400
    Y no se crea ninguna evaluación

  Escenario: Rechazo cuando no se envía ningún identificador
    Dado que no envío ni idSolicitante ni numeroDocumento
    Cuando intento calcular el score
    Entonces el sistema responde con estado 400
    Y no se crea ninguna evaluación

  Escenario: Exclusión de una variable de riesgo inactiva
    Dado que existe una variable de riesgo en estado INACTIVA con reglas asociadas
    Cuando calculo el score de un solicitante
    Entonces las reglas de esa variable no se consideran en el cálculo

  Escenario: Exclusión de reglas asociadas a una variable inactiva
    Dado que una variable de riesgo activa pasa a estado INACTIVA
    Cuando calculo el score de un solicitante después del cambio
    Entonces ninguna regla asociada a esa variable aporta puntaje ni aparece en el detalle

  Escenario: Evaluación de una condición numérica cumplida
    Dado que existe una regla activa sobre una variable numérica cuya condición se cumple con los datos del solicitante
    Cuando calculo el score
    Entonces el detalle de esa regla tiene condicionCumplida en verdadero y puntajeObtenido igual al puntaje de la regla

  Escenario: Evaluación de una condición numérica no cumplida
    Dado que existe una regla activa sobre una variable numérica cuya condición no se cumple con los datos del solicitante
    Cuando calculo el score
    Entonces el detalle de esa regla tiene condicionCumplida en falso y puntajeObtenido en cero

  Escenario: Evaluación de una condición categórica sobre HISTORIAL_CREDITICIO
    Dado que existe una regla activa con operador "=" sobre HISTORIAL_CREDITICIO
    Cuando el historial crediticio del solicitante coincide con el valor de la regla
    Entonces el detalle de esa regla tiene condicionCumplida en verdadero

  Esquema del escenario: Evaluación correcta de cada operador soportado
    Dado que existe una regla activa sobre una variable numérica con operador "<operador>"
    Cuando el valor real del solicitante es "<valorReal>" y la condición es "<valorCondicion>"
    Entonces condicionCumplida es "<resultadoEsperado>"

    Ejemplos:
      | operador | valorReal | valorCondicion | resultadoEsperado |
      | =        | 5000000   | 5000000        | true               |
      | >        | 6000000   | 5000000        | true               |
      | >=       | 5000000   | 5000000        | true               |
      | <        | 4000000   | 5000000        | true               |
      | <=       | 5000000   | 5000000        | true               |

  Escenario: Suma de múltiples reglas cumplidas y no cumplidas, incluyendo puntajes negativos
    Dado que existen varias reglas activas aplicables al solicitante, con puntajes positivos y negativos
    Cuando calculo el score
    Entonces el scoreTotal es igual a la suma de puntajeObtenido de todos los detalles

  Escenario: Regla no aplicable por pertenecer a una variable sin evaluar
    Dado que una regla está asociada a una variable de riesgo que no está en estado ACTIVA
    Cuando calculo el score
    Entonces esa regla no genera ningún detalle de evaluación

  Escenario: Persistencia de la evaluación y de todos los detalles considerados
    Dado que el cálculo del score se completa exitosamente
    Cuando consulto la base de datos
    Entonces existe un registro en EVALUACION con idSolicitante y fechaEvaluacion
    Y existe un registro en DETALLE_EVALUACION por cada regla considerada, cumplida o no

  Escenario: Preservación histórica después de editar una regla
    Dado que existe una evaluación previa cuyo detalle referencia una regla con un operador, valorCondicion y puntaje determinados
    Cuando esa regla se edita posteriormente cambiando su operador, valorCondicion o puntaje
    Entonces el detalle de la evaluación previa conserva el operador, valorCondicion y puntaje originalmente aplicados

  Escenario: Preservación histórica después de cambiar el estado de una variable
    Dado que existe una evaluación previa que consideró una regla de una variable ACTIVA
    Cuando esa variable se desactiva o reactiva posteriormente
    Entonces el resultado de la evaluación previa no cambia

  Escenario: Rollback ante fallo de persistencia
    Dado que el cálculo del score es válido pero ocurre un error al persistir los detalles
    Cuando se ejecuta la operación
    Entonces no queda ninguna EVALUACION ni ningún DETALLE_EVALUACION parcial registrados

  Escenario: Formato uniforme de error ante datos inválidos
    Dado que envío datos inválidos para calcular un score
    Cuando intento realizar el cálculo
    Entonces la respuesta de error contiene únicamente los campos errorCode, message, details y traceId
    Y la respuesta no contiene información de la traza técnica interna (stack trace)

  Escenario: Trazabilidad de la solicitud mediante X-Trace-Id
    Dado que envío una solicitud de cálculo sin el encabezado X-Trace-Id
    Cuando calculo el score de un solicitante
    Entonces la respuesta incluye el encabezado X-Trace-Id con un identificador generado por el sistema

  Escenario: Ingresos mensuales en cero con una regla activa de NIVEL_ENDEUDAMIENTO
    Dado que el solicitante tiene ingresos_mensuales igual a 0
    Y existe al menos una regla ACTIVA asociada a la variable NIVEL_ENDEUDAMIENTO
    Cuando intento calcular el score
    Entonces el sistema responde con estado 409
    Y no se crea ninguna evaluación ni ningún detalle de evaluación
