# CLAUDE.md

Reglas de trabajo para asistir en el desarrollo del **Motor de Scoring de Riesgo Crediticio**. Estas reglas son de obligatorio cumplimiento para cualquier trabajo realizado en este repositorio.

## 1. Descripción del proyecto

El Motor de Scoring de Riesgo Crediticio es un backend encargado de gestionar solicitantes de crédito, variables de riesgo y reglas de scoring, y de calcular y almacenar el historial de evaluaciones crediticias resultantes de aplicar dichas reglas sobre un solicitante.

## 2. Tecnologías actuales

* Java 21
* Spring Boot (spring-boot-starter-parent) con Spring Web MVC, Spring Data JPA, Spring Validation, Spring Security y Spring Boot Actuator
* Maven (con Maven Wrapper)
* PostgreSQL (base de datos de ejecución)
* H2 (solo para pruebas, perfil `test`)
* Flyway (migraciones de base de datos)
* springdoc-openapi (documentación OpenAPI/Swagger)
* Lombok
* Docker (previsto para el entorno de contenedores)

No se debe agregar, quitar ni actualizar dependencias o versiones sin justificación explícita ligada a una HU o a una instrucción directa.

## 3. Arquitectura general

* **Nivel de sistema**: la arquitectura del proyecto es un **monolito modular**. Todo el backend se despliega como una única aplicación, pero internamente está dividido en módulos de negocio independientes y desacoplados entre sí.
* **Organización estructural**: la organización del código sigue **Package by Feature**. Cada funcionalidad de negocio vive en su propio paquete de alto nivel bajo `com.crediticio` (`applicants`, `riskvariables`, `scoring`, `evaluations`), en lugar de agrupar el código por tipo técnico a nivel global.
* **Organización interna de cada módulo**: dentro de cada módulo se aplican los principios de **Ports & Adapters (arquitectura hexagonal)** para separar dominio, aplicación e infraestructura: el dominio y la aplicación definen contratos mediante `ports`, y la `infrastructure` provee los adaptadores concretos (entrada y salida) que implementan o consumen esos contratos.

No se debe introducir otro estilo arquitectónico (por ejemplo, Package by Layer a nivel global, microservicios, CQRS, event sourcing, etc.) sin que exista una justificación explícita y aprobación previa.

## 4. Módulos de negocio

Los módulos de negocio actuales son:

* `applicants`: gestión de solicitantes.
* `riskvariables`: gestión de variables de riesgo.
* `scoring`: gestión de reglas de scoring y cálculo del puntaje.
* `evaluations`: gestión del historial de evaluaciones.

Componentes transversales:

* `config`: configuración general de la aplicación (seguridad, OpenAPI, beans compartidos, etc.).
* `shared`: funcionalidades compartidas entre módulos (`audit`, `exception`, `response`, `util`).

No se deben crear nuevos módulos de negocio ni componentes transversales que no estén contemplados en la HU actual sin aprobación explícita.

## 5. Organización interna de cada módulo

Cada módulo de negocio (`applicants`, `riskvariables`, `scoring`, `evaluations`) sigue esta estructura interna:

* `domain`: entidades de dominio, invariantes y lógica de negocio pura, sin dependencias de frameworks de infraestructura.
* `application`: casos de uso / servicios de aplicación que orquestan el dominio, y el subpaquete `application/dto` con los DTO de entrada y salida.
* `ports`: interfaces que definen los contratos de entrada (`ports/input`, usados por la capa de aplicación) y de salida (`ports/output`, implementados por la infraestructura).
* `infrastructure`: adaptadores concretos, separados en `infrastructure/input` (por ejemplo, controladores REST) e `infrastructure/output` (por ejemplo, repositorios JPA, clientes externos).

Toda nueva clase debe ubicarse en la capa que le corresponde según esta organización. No se debe mezclar responsabilidades de capas distintas en una misma clase.

## 6. Principios de diseño, GRASP y patrones de diseño

Aplicar de forma pertinente (sin sobrediseñar). Ningún principio o patrón de esta sección es de uso obligatorio: se utilizan únicamente cuando resuelven una necesidad concreta y mejoran la separación de responsabilidades, el acoplamiento, la cohesión o la extensibilidad del código.

### Principios de diseño — SOLID

* **SRP**: cada clase tiene una única responsabilidad y una única razón de cambio.
* **OCP**: extender comportamiento mediante nuevas implementaciones de puertos/interfaces, evitando modificar código estable ya probado.
* **LSP**: las implementaciones de una interfaz de puerto deben ser sustituibles sin alterar el comportamiento esperado por quien las consume.
* **ISP**: los puertos (`ports/input`, `ports/output`) deben ser específicos y pequeños, no interfaces "todo en uno".
* **DIP**: la capa de aplicación y dominio dependen de abstracciones (`ports`), nunca de implementaciones concretas de infraestructura.

### Patrones de asignación de responsabilidades — GRASP

* **Information Expert**: asignar la responsabilidad a la clase que tiene la información necesaria para cumplirla (por ejemplo, el cálculo de un detalle de evaluación debe residir en el dominio, no en el controlador).
* **Creator**: quien agrega, contiene o usa estrechamente los datos de un objeto es responsable de crearlo.
* **Controller**: los adaptadores de entrada (controladores REST) delegan la lógica a los servicios de aplicación; no contienen reglas de negocio.
* **Low Coupling**: mantener dependencias mínimas y explícitas entre módulos y capas, mediadas por `ports`.
* **High Cohesion**: cada clase y módulo se mantiene enfocado en una responsabilidad claramente relacionada.
* **Indirection**: usar los puertos como intermediarios para desacoplar la aplicación de la infraestructura concreta (JPA, clientes HTTP, etc.).
* **Polymorphism**: variar el comportamiento según el tipo mediante distintas implementaciones de una misma interfaz de puerto, en lugar de condicionales por tipo.
* **Protected Variations**: aislar los puntos de variación previsibles (persistencia, integraciones externas, algoritmos de scoring) detrás de `ports`, de modo que los cambios en infraestructura o en reglas de negocio no impacten el dominio ni la aplicación.

### Patrones de diseño

Se consideran válidos para la arquitectura y el dominio actual únicamente los siguientes patrones, aplicados solo cuando exista una necesidad concreta:

* **Repository**: para abstraer el acceso a persistencia detrás de un puerto de salida (`ports/output`), independizando el dominio/aplicación del mecanismo de almacenamiento concreto (JPA/PostgreSQL, H2 en pruebas).
* **Adapter**: para implementar los puertos (`ports/input` y `ports/output`) en la capa de `infrastructure`, conectando el dominio/aplicación con tecnologías externas (controladores REST, repositorios JPA, clientes externos).
* **Strategy**: únicamente cuando exista una necesidad real de encapsular algoritmos o comportamientos intercambiables, en particular en el motor de scoring (por ejemplo, distintas formas de evaluar una regla de scoring).

No se deben aplicar patrones adicionales (Factory, Builder, Observer, Facade, etc.) solo por completar una lista o por anticipación. Si una HU en desarrollo justifica la necesidad de otro patrón, debe justificarse explícitamente en función de esa HU antes de aplicarlo.

## 7. Uso de DTO

* Los controladores (`infrastructure/input`) reciben y devuelven exclusivamente DTO definidos en `application/dto`, nunca entidades de dominio directamente.
* Los DTO de entrada deben validarse con Bean Validation (`spring-boot-starter-validation`) cuando corresponda.
* La conversión entre DTO y entidades de dominio ocurre en la capa de aplicación (o en un mapper dedicado dentro de esa capa), no en el dominio ni en el controlador.
* No exponer campos internos o sensibles del dominio en los DTO de salida sin necesidad justificada por la HU.

## 8. Separación entre dominio, aplicación e infraestructura

* El **dominio** no depende de Spring, JPA, ni de ningún framework de infraestructura. Contiene entidades, value objects y reglas de negocio puras.
* La **aplicación** orquesta el dominio a través de los puertos, sin conocer detalles de infraestructura (por ejemplo, no debe conocer anotaciones JPA ni detalles HTTP).
* La **infraestructura** implementa los puertos de salida (persistencia, integraciones externas) y expone los puertos de entrada (controladores REST, etc.), traduciendo entre el mundo externo y la aplicación/dominio.
* Nunca se debe hacer referencia inversa: el dominio no debe importar clases de `infrastructure`, y la aplicación no debe importar detalles concretos de `infrastructure` (solo los `ports`).

## 9. Reglas para PostgreSQL y Flyway

* PostgreSQL es la base de datos objetivo de ejecución; H2 se usa únicamente en pruebas (perfil `test`).
* Todo cambio de esquema (creación o modificación de tablas, índices, restricciones) se implementa mediante una nueva migración de **Flyway** versionada en `src/main/resources/db/migration`, siguiendo la convención de nombres de Flyway (`V{version}__{descripcion}.sql`).
* Nunca modificar una migración de Flyway ya aplicada/versionada previamente; los cambios posteriores requieren una nueva migración.
* `spring.jpa.hibernate.ddl-auto` no debe usarse para gestionar el esquema en el perfil principal/producción; el esquema se gestiona vía Flyway. En el perfil `test` puede usarse `create-drop` sobre H2 con Flyway deshabilitado, tal como está configurado actualmente.
* No deshabilitar ni omitir Flyway fuera del perfil de pruebas sin autorización explícita.

## 10. Modelo de scoring basado en reglas y puntos

* El scoring se calcula aplicando un conjunto de **reglas de scoring** (`regla_scoring`) sobre las variables de riesgo (`riesgo`) asociadas a un solicitante.
* Cada regla evaluada que aplica a un solicitante genera un **detalle de evaluación** (`detalle_evaluacion`) con los puntos que aporta esa regla.
* El puntaje total de una evaluación es la agregación (suma) de los puntos de todos sus detalles asociados.
* La lógica de aplicación de reglas y cálculo de puntos pertenece al módulo `scoring` (dominio/aplicación), no a los controladores ni a la capa de persistencia.

## 11. Modelo de datos: cinco tablas

El modelo relacional actual contempla las siguientes cinco tablas:

1. `solicitante`: datos del solicitante de crédito.
2. `riesgo`: variables de riesgo evaluables.
3. `regla_scoring`: reglas de negocio que asignan puntos según variables de riesgo.
4. `evaluacion`: registro de una evaluación de scoring realizada sobre un solicitante.
5. `detalle_evaluacion`: detalle de cada regla aplicada dentro de una evaluación, con los puntos otorgados por esa regla.

No se deben crear tablas adicionales, ni modificar la finalidad de estas cinco, sin que la HU en curso lo requiera explícitamente y sin aprobación.

## 12. Regla sobre `score_total`

`score_total` de una evaluación **no debe almacenarse como columna persistida** si su valor puede derivarse (calcularse) a partir de la suma de los puntos registrados en `detalle_evaluacion`. Debe calcularse en tiempo de consulta/uso (dominio o aplicación) a partir de los detalles, evitando datos derivados duplicados y posibles inconsistencias. Si en algún momento se considera necesario persistirlo (por ejemplo, por performance), esto requiere justificación explícita y aprobación antes de implementarse.

## 13. Reglas de validación y pruebas

* Toda nueva funcionalidad debe incluir pruebas automatizadas (unitarias y/o de integración, según corresponda) que cubran el comportamiento esperado y los criterios de aceptación de la HU.
* Las pruebas de integración que requieran base de datos usan el perfil `test` con H2 en memoria, tal como está configurado en `src/test/resources/application-test.properties`.
* Las pruebas deben ejecutarse antes de dar por terminada una HU (`./mvnw test` o `./mvnw.cmd clean test`).
* No se deben eliminar, comentar, deshabilitar ni modificar pruebas existentes con el fin de ocultar errores o hacer pasar el build artificialmente. Si una prueba falla, se investiga y corrige la causa raíz.
* Los datos y variables de entrada deben validarse en la capa correspondiente (DTO de entrada con Bean Validation, e invariantes de negocio en el dominio).

## 14. Reglas de Git y ramas

* Todo trabajo de una HU se realiza en una rama dedicada creada a partir de `main` (por ejemplo, `feature/<nombre-corto-de-la-hu>`).
* No se debe trabajar directamente commits de código de HU sobre `main`.
* Cada rama debe mantenerse enfocada en una sola HU o tarea.

## 15. No modificar `main` directamente

Está prohibido modificar la rama `main` directamente. Todo cambio de código llega a `main` mediante el flujo de ramas y revisión definido por el equipo/María Alejandra, nunca mediante commits directos sobre `main`.

## 16. Autorización explícita para commit y push

No se debe ejecutar `git commit` ni `git push` bajo ninguna circunstancia sin la autorización explícita de María Alejandra en la conversación, para esa acción concreta. Una autorización dada para un commit/push no habilita commits o pushes futuros; debe solicitarse de nuevo cada vez.

## 17. No implementar fuera del alcance de la HU actual

No se debe implementar ninguna funcionalidad, endpoint, entidad, regla o mejora que no esté explícitamente contemplada en la Historia de Usuario (HU) que se está trabajando en el momento. Cualquier necesidad adicional detectada se reporta y se espera indicación, no se implementa de forma proactiva.

## 18. No ocultar errores mediante pruebas

No se deben eliminar ni modificar pruebas existentes (ni escribir pruebas debilitadas a propósito) para hacer que un build o una suite de pruebas pase ocultando un error real. Los errores detectados por las pruebas deben resolverse en el código de producción.

## 19. Flujo obligatorio para cada Historia de Usuario (HU)

Para cada HU se debe seguir, en orden, el siguiente flujo:

1. Leer la especificación completa de la HU.
2. Revisar los criterios de aceptación de la HU.
3. Analizar el estado actual del proyecto relevante para la HU (estructura, código existente, dependencias).
4. Proponer un plan de implementación.
5. Esperar la aprobación explícita del plan antes de escribir o modificar código.
6. Implementar el plan aprobado.
7. Crear o actualizar las pruebas automatizadas correspondientes.
8. Ejecutar las pruebas y verificar que pasen.
9. Verificar que se cumplen todos los criterios de aceptación de la HU.
10. Revisar los cambios realizados (diff) antes de darlos por finalizados.
11. Actualizar el progreso del proyecto (por ejemplo, en `claude-progress.md` cuando exista).

No se debe saltar, reordenar de forma que pierda su intención, ni omitir ninguno de estos pasos.

## 20. Ambigüedad en los requisitos

Si durante el análisis de una HU se identifica una ambigüedad importante en los requisitos o en los criterios de aceptación, se debe **detener el trabajo** y solicitar aclaración explícita antes de continuar, en lugar de asumir o inventar el comportamiento esperado.

## 21. Revisar antes de modificar

Antes de modificar cualquier archivo existente, se debe revisar su implementación actual (y la de los archivos relacionados que dependan de él o de los que dependa) para entender el contexto y evitar romper comportamiento existente o duplicar lógica.

## 22. Cambios pequeños y acotados a la HU

Los cambios realizados deben ser pequeños y estar directamente relacionados con la HU en curso. No se deben incluir refactorizaciones, limpiezas o mejoras no solicitadas junto con el cambio de la HU, aunque se detecten oportunidades de mejora; estas se reportan por separado.

## Reglas adicionales importantes

* No inventar requisitos funcionales que no estén especificados.
* No implementar funcionalidades de forma automática o anticipada.
* No hacer `commit` ni `push` sin autorización explícita (ver regla 16).
* No cambiar la arquitectura existente sin justificarlo y sin aprobación explícita.
* No ejecutar cambios adicionales fuera de lo solicitado en la interacción actual.
