# language: es
Característica: Crear y publicar una política de clasificación
  Como administrador de riesgo
  Quiero definir y publicar políticas de clasificación del riesgo crediticio
  Para establecer cómo se asigna una categoría a cada puntaje obtenido

  # Endpoints (HU10): GET /api/v1/politicas-clasificacion/vigente para ADMINISTRADOR_RIESGO y
  # ANALISTA_CREDITO; POST /api/v1/politicas-clasificacion solo para ADMINISTRADOR_RIESGO.

  Antecedentes:
    Dado que existe la configuración única de la política de clasificación vigente

  Escenario: Publicación exitosa de la primera política
    Dado que todavía no existe una política de clasificación vigente
    Cuando publico una política con los rangos BAJO [-100, 29], MEDIO [30, 79] y ALTO [80, 130]
    Entonces la política queda registrada con la versión 1
    Y la política queda registrada con exactamente tres rangos
    Y la política publicada pasa a ser la política vigente
    Y la fecha de creación y la fecha de publicación corresponden al mismo instante

  Escenario: Publicación de una nueva versión conservando la anterior
    Dado que existe una política de clasificación vigente
    Cuando publico una nueva política válida
    Entonces la nueva política queda registrada con la versión siguiente
    Y la nueva política pasa a ser la política vigente
    Y la política anterior y sus rangos permanecen sin modificaciones

  Escenario: Aceptación de los límites inclusivos del intervalo
    Cuando publico una política cuyos rangos empiezan en -100 y terminan en 130
    Entonces la política se publica correctamente

  Esquema del escenario: Rechazo de una política con rangos inválidos
    Cuando intento publicar una política con <caso>
    Entonces la política es rechazada indicando todos los errores encontrados
    Y no se registra ninguna política ni ningún rango
    Y la política vigente no cambia

    Ejemplos:
      | caso                                                    |
      | un límite inferior menor que -100                       |
      | un límite superior mayor que 130                        |
      | un límite inferior mayor que su límite superior         |
      | dos rangos que se superponen                            |
      | un hueco entre dos rangos consecutivos                  |
      | rangos que no cubren todo el intervalo de -100 a 130    |
      | una categoría duplicada                                 |
      | una categoría faltante                                  |
      | un número de rangos distinto de tres                    |
      | un rango, una categoría o un límite sin informar        |

  Escenario: Reporte conjunto de errores
    Cuando intento publicar una política con varios errores a la vez
    Entonces la política es rechazada indicando cada uno de los errores, no solo el primero

  Escenario: Publicación fallida sin efectos parciales
    Dado que existe una política de clasificación vigente
    Cuando la publicación de una nueva política falla después de comenzar a guardarse
    Entonces no queda registrada ninguna parte de la nueva política
    Y la política vigente no cambia

  Escenario: Publicaciones simultáneas
    Cuando dos publicaciones válidas se realizan al mismo tiempo
    Entonces ambas se registran una después de otra con versiones consecutivas
    Y la política vigente es la de versión más alta
    Y no existen versiones duplicadas

  Escenario: Inmutabilidad de una política publicada
    Dado que existe una política publicada
    Cuando necesito cambiar sus rangos
    Entonces debo publicar una nueva versión de la política
    Y la política publicada no ofrece ninguna operación de modificación ni de eliminación

  # La consulta se ejecuta mediante el caso de uso, sin endpoint HTTP (Fase 1).
  Escenario: Consulta de la política vigente
    Dado que existe una política de clasificación vigente
    Cuando consulto la política vigente
    Entonces obtengo su versión, su fecha de creación, su fecha de publicación y sus tres rangos ordenados por límite inferior

  Escenario: Consulta de la política vigente después de publicar una nueva versión
    Dado que existe una política de clasificación vigente
    Cuando publico una nueva política válida y consulto la política vigente
    Entonces obtengo la nueva versión
    Y la versión anterior permanece sin modificaciones

  Escenario: Consulta sin política vigente
    Dado que todavía no existe una política de clasificación vigente
    Cuando consulto la política vigente
    Entonces se informa explícitamente que no existe una política de clasificación vigente

  # CA09 exige que la política esté completa y sin categorías duplicadas; no exige un orden fijo entre categorías.
  Escenario: Publicación de una política válida sin un orden fijo entre categorías
    Cuando publico una política con los rangos ALTO [-100, 29], BAJO [30, 79] y MEDIO [80, 130]
    Entonces la política se publica correctamente
    Y cada categoría conserva el rango con el que fue definida

  # CA14: columna publicado_por de V7, tomada del claim sub del JWT. Validado sobre PostgreSQL el
  # 2026-10-18 por D1 (PoliticaClasificacionV7MigracionIT) y D2 (PoliticaClasificacionRepositoryAdapterIT).
  Escenario: Registro del autor de la publicación
    Cuando un administrador de riesgo autenticado publica una política
    Entonces la publicación registra su autor

  Escenario: Consulta de la política vigente por ambos roles
    Dado que existe una política de clasificación vigente
    Cuando un administrador de riesgo o un analista de crédito autenticado consulta la política vigente
    Entonces el sistema retorna la política vigente con sus rangos

  Escenario: Publicación restringida al rol ADMINISTRADOR_RIESGO
    Dado que un usuario autenticado no tiene el rol ADMINISTRADOR_RIESGO
    Cuando intenta publicar una política de clasificación
    Entonces el sistema rechaza la operación por falta de permisos
