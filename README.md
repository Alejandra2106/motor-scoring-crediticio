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

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | Sí | `postgresql://host:puerto/base` (Spring agrega el prefijo `jdbc:`) |
| `DB_USERNAME`, `DB_PASSWORD` | Sí | Rol de ejecución (sin propiedad de las tablas ni privilegios administrativos; `docs/operacion/roles-scoring-api.sql`) |
| `SCORING_FLYWAY_MIGRAR_AL_ARRANCAR` | No | `false` por defecto: Flyway solo valida y, si hay migraciones pendientes, la aplicación no arranca y no aplica ninguna. `true` solo en un despliegue controlado (`docs/operacion/despliegue-v10-v11.md`) |
| `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD` | Solo al migrar | Rol propietario de las tablas, que aplica las migraciones |
| `SCORING_SAGA_HABILITADA` y variables `SCORING_SERVICE_JWT_*`, `EVALUATION_QUERY_API_BASE_URL` | Con la saga (HU14) | Ver `application.properties` y el README de `evaluation-query-api` |
| `AUTH_JWT_ISSUER` | Sí | Emisor de los JWT; debe ser el mismo valor configurado en `auth-api` (allí, `auth-api` por defecto) |
| `SCORING_JWT_PUBLIC_KEY_LOCATION` | Sí | Ubicación de la clave pública RS256 de `auth-api`, p. ej. `file:/ruta/jwt-publica.pem` |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes del frontend separados por coma (por defecto `http://localhost:3000`) |

Sin `AUTH_JWT_ISSUER` o sin una clave pública válida la aplicación no arranca, y tampoco si la base tiene
migraciones pendientes y no se pidió migrar (protección contra la aplicación accidental de V10 y V11). Con el perfil `dev`
(`SPRING_PROFILES_ACTIVE=dev`) se habilita Swagger; en cualquier otro perfil está deshabilitado.

## Autenticación y autorización (HU10)

Todos los endpoints de negocio exigen un JWT de acceso emitido por `auth-api` en la cabecera
`Authorization: Bearer <token>`. El token se valida localmente, sin llamar a `auth-api`. La matriz de
permisos por rol y endpoint está en `docs/security/matriz-permisos-scoring-api.md`.

### Clave pública

- `auth-api` firma los tokens con su clave privada (RS256). La clave privada **solo** se instala en
  `auth-api` y nunca se copia a esta API ni se versiona.
- `scoring-api` recibe únicamente la clave pública X.509 en formato PEM (`-----BEGIN PUBLIC KEY-----`)
  del mismo par que usa `auth-api` (su variable `AUTH_JWT_PUBLIC_KEY_LOCATION`). El procedimiento de
  generación del par está descrito en el README de `auth-api`.
- El archivo PEM se guarda fuera del repositorio (`.gitignore` no debe permitir versionarlo) y su ruta se
  indica en `SCORING_JWT_PUBLIC_KEY_LOCATION`.
- Rotación: instalar primero la nueva clave pública aquí, cambiar después las claves de `auth-api`. Esta
  API valida con una sola clave a la vez, por lo que durante el cambio pueden rechazarse tokens emitidos
  con la clave anterior hasta su expiración (como máximo 15 minutos).

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
