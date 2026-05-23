# MTGDeckbuilder

Backend para gestion de colecciones y mazos de Magic: The Gathering, construido con Spring Boot y PostgreSQL.

## Vision general

El proyecto esta orientado a:

- Gestionar cartas, sets, formatos, usuarios, mazos y tags.
- Aplicar reglas de construccion de mazos (cantidad, legalidad por formato, secciones).
- Exponer una API REST definida con OpenAPI.
- Soportar embeddings vectoriales con pgvector para capacidades semanticas.

## Stack tecnico

- Java 21
- Spring Boot 3.5.6
- Spring Web + Spring Data JPA
- PostgreSQL + pgvector
- Maven
- OpenAPI 3 + code generation (`scs-multiapi-maven-plugin`)
- JUnit 5, Mockito, AssertJ, Testcontainers

## Estructura del proyecto

```text
MTGDeckbuilder/
  infra/                                  # Docker Compose y Dockerfile
  src/main/java/com/deckbuilder/mtgdeckbuilder/
	application/                          # Casos de uso y logica de negocio
	contract/                             # Controllers REST
	infrastructure/                       # Repositorios y adaptadores tecnicos
	model/                                # Entidades y objetos de dominio
  src/main/resources/
	api/openapi.yml                       # Contrato API
	scripts/                              # Schema SQL y scripts de soporte
	application*.properties               # Config por entorno
  src/test/                               # Tests por capa + integracion
  docs/                                   # Documentacion de onboarding/arquitectura
```

## Quick start

Usa comandos genéricos de Maven para mantener la guia independiente del sistema operativo.

### 1) Levantar infraestructura local

```bash
cd infra
docker compose up -d
cd ..
```

Notas:

- Segun `infra/docker-compose.yml`, la base de datos se publica en `localhost:5433`.
- El contenedor de app publica `8080`.

### 2) Compilar y ejecutar tests

```bash
mvn clean install
mvn test
```

### 3) Regenerar fuentes OpenAPI (si cambias el contrato)

```bash
mvn generate-sources
```

## Configuracion

Referencia principal: `src/main/resources/application.properties`.

Variables relevantes:

- `DB_URL` (default `jdbc:postgresql://localhost:5433/mtg_db`)
- `DB_USERNAME` (default `user`)
- `DB_PASSWORD`
- `PORT` (default `8080`)
- `JWT_SECRET`
- `JWT_EXPIRATION`

## API

- Contrato fuente: `src/main/resources/api/openapi.yml`
- Server base definido en OpenAPI: `/api/v1`

Recurso principales del contrato:

- `/cards`
- `/decks`
- `/users`
- `/formats`
- `/sets`
- `/tags`

## Base de datos y reglas criticas

Script principal: `src/main/resources/scripts/01-create-schema.sql`.

Puntos clave:

- Uso de `VECTOR(1536)` para embeddings en `cards.embedding`.
- Validacion de reglas de mazo mediante trigger `check_card_quantity`.
- Constraints para secciones de deck: `main`, `sideboard`, `maybeboard`.
- Actualizacion automatica de `decks.last_modification` desde trigger.

## Documentacion

- Contexto tecnico completo: `docs/TECHNICAL_AGENT_CONTEXT.md`
- Onboarding rapido: `docs/AGENT_QUICKSTART.md`
- Decisiones de arquitectura (ADR): `docs/ARCHITECTURE_DECISIONS.md`

## Contribucion (reglas practicas)

- Mantener separacion por capas (`contract`, `application`, `infrastructure`, `model`).
- No editar manualmente codigo generado en `target/generated-sources/`.
- Si cambias OpenAPI, regenerar fuentes y validar compilacion/tests.
- Si cambias reglas de deck, revisar consistencia entre Java y SQL trigger.

## Estado y siguientes mejoras

- Documentar completamente autenticacion/autorizacion cuando se cierre el diseno.
- Estandarizar naming de parametros de paginacion en OpenAPI.
- Revisar estrategia unificada de schema base vs schema enhanced.
