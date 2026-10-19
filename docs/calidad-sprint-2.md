# Evidencia de calidad e integración continua — sprint 2

## Alcance de esta entrega

La rama de trabajo incorpora cuatro escenarios de aceptación Karate para la historia de registrar y consultar solicitantes. Comprueban la respuesta HTTP y datos de negocio observables: registro y consulta, rechazo de documento duplicado, validación de entrada y solicitante inexistente. Se ejecutan junto con las pruebas unitarias mediante Maven. No son pruebas de carga ni sustituyen la suite de integración con PostgreSQL.

La integración continua genera el JAR ejecutable, los resultados JUnit/Karate y el reporte JaCoCo. Una segunda tarea ejecuta la suite completa con PostgreSQL y conserva sus reportes incluso si falla. El análisis de SonarCloud y su quality gate se ejecutan cuando la configuración del proyecto está disponible; el flujo no inventa un resultado de SonarCloud si faltan credenciales.

## Ejecución local comprobada el 19 de octubre de 2026

Comando: `./mvnw clean verify -DskipITs` (en Windows, `.\mvnw.cmd`).

| Evidencia | Resultado |
| --- | ---: |
| Pruebas Surefire aprobadas | 268 de 268 |
| Escenarios Karate incluidos en Surefire | 4 de 4 |
| Líneas cubiertas por JaCoCo | 833 de 1011 (82,4 %) |
| Ramas cubiertas por JaCoCo | 123 de 132 (93,2 %) |
| JAR ejecutable | `target/motor-scoring-crediticio-0.0.1-SNAPSHOT.jar` |
| Reporte JaCoCo XML | `target/site/jacoco/jacoco.xml` |
| Reporte Karate | `target/karate-reports/` |

Estas cifras corresponden a esta ejecución local con las pruebas de integración omitidas. La cobertura de SonarCloud, la deuda técnica, los code smells y el estado del quality gate deben leerse de un análisis real en SonarCloud; no se deducen de JaCoCo.

## Configuración pendiente de SonarCloud

1. Crear o importar en SonarQube Cloud el repositorio GitHub que recibirá la rama o el PR.
2. En GitHub Actions, definir el secreto `SONAR_TOKEN` y las variables `SONAR_PROJECT_KEY` y `SONAR_ORGANIZATION` del proyecto real.
3. Ejecutar el flujo `Calidad y artefacto` y abrir el análisis de SonarCloud. Registrar la URL, fecha, quality gate, cobertura, duplicación, complejidad, code smells y deuda técnica en tiempo de remediación.
4. Conservar una captura del panel de SonarCloud donde se lean los valores; no reutilizar números de otros equipos.

## Trazabilidad para la entrega

La evidencia completa debe vincular el commit y su pull request con la ejecución de GitHub Actions, el JAR publicado como artefacto, los reportes de pruebas y el análisis de SonarCloud. El profesor pidió enviar la URL de GitHub y la de SonarCloud, acompañadas por presentación y video. La integración se propone mediante rama y pull request, sin escribir directamente en `main`.

Si la tarea de PostgreSQL falla, el flujo debe mostrar el fallo y conservar el reporte de Failsafe. No se debe presentar una ejecución local sin PostgreSQL como aprobación de esas pruebas.
