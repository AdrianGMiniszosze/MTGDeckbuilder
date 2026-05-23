# MTGDeckbuilder

Backend para gestión de colecciones y mazos de Magic: The Gathering, construido con Spring Boot y PostgreSQL.

## Descripción

API REST que permite a los usuarios crear y gestionar mazos, explorar cartas y aplicar las reglas de construcción de mazos de MTG (legalidad por formato, límites de copias, secciones main/sideboard/maybeboard). El sistema incluye soporte para embeddings vectoriales con `pgvector` para búsquedas semánticas de cartas.

## Stack

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje principal |
| Spring Boot 3.5.6 | Framework web y de aplicación |
| Spring Data JPA | Capa de persistencia |
| PostgreSQL + pgvector | Base de datos + embeddings vectoriales |
| OpenAPI 3 | Contrato de la API (code-first generation) |
| JUnit 5 + Mockito + AssertJ | Tests unitarios |
| Testcontainers | Tests de integración contra PostgreSQL real |
| Docker Compose | Entorno local de desarrollo |

## Estructura del proyecto

```text
MTGDeckbuilder/
├── infra/                        # Docker Compose y Dockerfile
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/deckbuilder/mtgdeckbuilder/
│   │   │       ├── application/      # Servicios y lógica de negocio
│   │   │       ├── contract/         # Controllers REST
│   │   │       ├── infrastructure/   # Repositorios JPA y adaptadores
│   │   │       └── model/            # Entidades y objetos de dominio
│   │   └── resources/
│   │       ├── api/openapi.yml       # Contrato de la API
│   │       ├── scripts/              # Schema SQL
│   │       └── application*.properties
│   └── test/                         # Tests por capa e integración
├── docs/                             # Documentación técnica y decisiones
└── .github/workflows/                # CI/CD con GitHub Actions
```

## Quick start

### 1. Requisitos previos

- Java 21
- Docker (para la base de datos local y los tests de integración)
- Maven 3.9+ (o usar el wrapper `./mvnw` incluido)

### 2. Arrancar la infraestructura local

```bash
cd infra
docker compose up -d
```

Esto levanta PostgreSQL (con extensión `pgvector`) en `localhost:5433` y la aplicación en `localhost:8080`.

### 3. Compilar el proyecto

```bash
./mvnw clean compile
```

La primera compilación incluye la generación de código desde `openapi.yml` automáticamente.

### 4. Ejecutar tests

**Tests unitarios** (rápidos, sin dependencias externas):

```bash
./mvnw test "-Dtest=*Test,!*IntegrationTest,!CardLegalityRepositoryTest,!CardInDeckRepositoryExtendedTest" -DfailIfNoTests=false
```

**Tests de integración** (requieren Docker activo, recomendado en Linux o CI):

```bash
./mvnw test -Dtest="*IntegrationTest,CardLegalityRepositoryTest,CardInDeckRepositoryExtendedTest" -DfailIfNoTests=false
```

Los tests de integración usan Testcontainers para levantar automáticamente una instancia de PostgreSQL con pgvector. No hace falta configurar nada adicional si Docker está disponible.

### 5. Regenerar código desde el contrato OpenAPI

Si modificas `src/main/resources/api/openapi.yml`:

```bash
./mvnw generate-sources
```

## Configuración

La configuración se gestiona mediante variables de entorno. Los valores por defecto sirven para desarrollo local con el Docker Compose incluido.

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/mtg_db` | URL JDBC de la base de datos |
| `DB_USERNAME` | `user` | Usuario de la base de datos |
| `DB_PASSWORD` | — | Contraseña (requerida) |
| `PORT` | `8080` | Puerto del servidor |
| `JWT_SECRET` | valor de desarrollo | Clave para firmar tokens JWT |
| `JWT_EXPIRATION` | `86400000` | Expiración del token en ms (24h) |

Referencia completa: `src/main/resources/application.properties`.

## API

El contrato completo de la API está definido en `src/main/resources/api/openapi.yml`.

Recursos principales:

| Recurso | Descripción |
|---|---|
| `GET/POST /cards` | Listado y búsqueda de cartas |
| `GET/POST/PUT/DELETE /decks` | CRUD de mazos |
| `POST /decks/{id}/cards` | Añadir cartas a un mazo |
| `GET/POST /users` | Gestión de usuarios |
| `GET /formats` | Listado de formatos disponibles |
| `GET /sets` | Listado de sets |
| `GET/POST /tags` | Sistema de etiquetas de cartas |

## Base de datos

El schema se crea con el script `src/main/resources/scripts/01-create-schema.sql`. Los aspectos más relevantes para desarrollo:

- El campo `cards.embedding` es de tipo `VECTOR(1536)` y requiere la extensión `pgvector`.
- Las reglas de construcción de mazos se validan mediante un trigger SQL (`check_card_quantity`): legalidad por formato, límite de 4 copias, restricciones de mazo Commander, etc.
- La columna `decks.last_modification` se actualiza automáticamente con cada cambio.
- Los sections válidos para cartas en un mazo son: `main`, `sideboard`, `maybeboard`.

## CI/CD

El repositorio incluye dos workflows de GitHub Actions:

| Workflow | Disparador | Qué hace |
|---|---|---|
| `ci.yml` | PR y push a `main` | Codegen + compilación + tests unitarios + tests de integración |
| `build-check.yml` | PR a `main` | Verifica que el JAR se empaqueta correctamente |

Los tests de integración corren en `ubuntu-latest`, donde Docker está disponible de forma nativa y Testcontainers funciona sin configuración adicional.

## Contribuir

1. Seguir la arquitectura por capas: `contract` → `application` → `infrastructure`.
2. No editar manualmente código en `target/generated-sources/` (se regenera automáticamente).
3. Si cambias el contrato OpenAPI, ejecutar `./mvnw generate-sources` y verificar que compila.
4. Los PRs requieren que los checks de CI pasen antes de poder hacer merge.

## Documentación técnica

- Arquitectura y decisiones de diseño: [`docs/ARCHITECTURE_DECISIONS.md`](docs/ARCHITECTURE_DECISIONS.md)
- Contexto técnico detallado: [`docs/TECHNICAL_AGENT_CONTEXT.md`](docs/TECHNICAL_AGENT_CONTEXT.md)
- Guía de onboarding rápido: [`docs/AGENT_QUICKSTART.md`](docs/AGENT_QUICKSTART.md)
