# Motor de Scoring de Riesgo Crediticio

Backend para la gestión y evaluación del riesgo crediticio mediante variables y reglas de scoring.

## Tecnologías

* Java 21
* Spring Boot
* Maven
* PostgreSQL
* Spring Data JPA
* Spring Validation
* Spring Security
* Flyway
* Docker

## Requisitos

Antes de ejecutar el proyecto localmente se requiere tener instalado:

* JDK 21
* Git
* PostgreSQL
* Docker Desktop (opcional para el entorno de contenedores)

Verificar Java:

```bash
java -version
```

Verificar Git:

```bash
git --version
```

## Ejecución local

Clonar el repositorio:

```bash
git clone <URL_DEL_REPOSITORIO>
```

Ingresar al proyecto:

```bash
cd motor-scoring-crediticio
```

En Windows ejecutar:

```powershell
.\mvnw.cmd clean test
```

Para iniciar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación estará disponible en:

```text
http://localhost:8080
```

## Estructura

El proyecto está organizado como un monolito modular mediante Package by Feature.

Los principales módulos de negocio son:

* `applicants`: gestión de solicitantes.
* `riskvariables`: gestión de variables de riesgo.
* `scoring`: gestión de reglas y cálculo del scoring.
* `evaluations`: gestión del historial de evaluaciones.

Los componentes transversales se encuentran en:

* `config`: configuración general de la aplicación.
* `shared`: funcionalidades compartidas entre módulos.

## Estado del proyecto

Proyecto base generado con Spring Initializr.

Actualmente contiene el esqueleto arquitectónico inicial y la configuración base para comenzar el desarrollo de las historias de usuario.
