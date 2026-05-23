# MTGDeckbuilder - Contexto Tecnico para Agentes

> Ultima actualizacion: 2026-05-23  
> Ubicacion recomendada: `docs/TECHNICAL_AGENT_CONTEXT.md`

## 1. Objetivo del documento

Este archivo resume la arquitectura, flujos criticos, convenciones y puntos de riesgo de `MTGDeckbuilder` para que cualquier agente pueda entrar al repositorio con contexto tecnico operativo.

## 2. Stack y herramientas clave

- Java 21
- Spring Boot 3.5.6
- Spring Web + Spring Data JPA
- PostgreSQL con extension `pgvector` (imagen `ankane/pgvector` en Docker)
- OpenAPI 3.0.3 como contrato de API (`src/main/resources/api/openapi.yml`)
- Generacion de codigo OpenAPI via `scs-multiapi-maven-plugin`
- MapStruct + Lombok
- Tests: JUnit 5, Mockito, AssertJ, Testcontainers

Fuente principal: `pom.xml`.

## 3. Estructura del repositorio (practica)

Raiz:

- `pom.xml` - build Maven, dependencias, plugins y codegen
- `src/main/java/com/deckbuilder/mtgdeckbuilder/`
  - `application/` - servicios de negocio
  - `contract/` - controllers REST y mapeo de entrada/salida
  - `infrastructure/` - repositorios y componentes tecnicos
  - `model/` - entidades/objetos de dominio
- `src/main/resources/`
  - `api/openapi.yml` - contrato API
  - `scripts/` - schema y SQL operativo
  - `application*.properties` - configuraciones por entorno
- `src/test/java/com/deckbuilder/mtgdeckbuilder/`
  - tests por capa y pruebas de integracion
- `infra/`
  - `docker-compose.yml` - runtime local (db + app)

## 4. Arquitectura y capas

### 4.1 Capa `contract`

Responsabilidades:

- Exponer endpoints HTTP.
- Recibir parametros, body y devolver codigos/respuestas.
- Delegar a servicios de `application`.

Controladores detectados:

- `CardController`
- `DeckController`
- `FormatController`
- `SetController`
- `TagController`
- `UserController`

### 4.2 Capa `application`

Responsabilidades:

- Casos de uso de negocio.
- Reglas de dominio y coordinacion entre repositorios.

Servicios detectados:

- `CardService`
- `CardTagService`
- `DeckService`
- `DeckValidationService`
- `FormatService`
- `SetService`
- `TagService`
- `UserService`
- utilitario/reglas: `CompanionRules`

### 4.3 Capa `infrastructure`

Responsabilidades:

- Persistencia y acceso a datos.
- Repositorios de entidades y componentes de soporte.

Repositorios detectados:

- `CardRepository`
- `CardTagRepository`
- `CardLegalityRepository`
- `CardInDeckRepository`
- `DeckRepository`
- `FormatRepository`
- `SetRepository`
- `TagRepository`
- `UserRepository`

### 4.4 Capa `model`

Responsabilidades:

- Entidades de dominio y estructuras de busqueda.

Modelos detectados:

- `Card`, `Deck`, `Format`, `Set`, `Tag`, `User`, `CardTag`
- apoyo de busqueda: `CardSearchCriteria`, `CardSearchResult`

## 5. Contrato API (OpenAPI)

Archivo fuente:

- `src/main/resources/api/openapi.yml`

Observaciones relevantes:

- Version OpenAPI: `3.0.3`
- Server base: `/api/v1`
- Recursos principales:
  - `/sets`
  - `/cards`
  - `/users`
  - `/decks`
  - `/formats`
  - `/tags`

Nota tecnica importante:

- En el spec, hay endpoints con parametros de paginacion como `pageSize/pageNumber` y otros como `pagesize/pagenumber`. Cualquier cambio de contrato debe validar consistencia de naming para evitar friccion en clientes.

## 6. Persistencia y base de datos

Script principal inspeccionado:

- `src/main/resources/scripts/01-create-schema.sql`

### 6.1 Tablas clave

- `sets`
- `cards` (incluye `embedding VECTOR(1536)`)
- `users`
- `formats`
- `decks`
- `tags`
- `card_legality`
- `card_deck`
- `card_tag`
- tablas de atributos M:N: `card_colors`, `card_color_identity`, `card_keywords`, `card_subtypes`

### 6.2 Reglas y constraints destacables

- FK con `ON DELETE CASCADE` en relaciones relevantes.
- Unicidad de variantes de carta: `(card_name, card_set, collector_number)`.
- `deck_type` restringido a `main`, `sideboard`, `maybeboard`.
- `card_deck.section` con el mismo dominio (`main`, `sideboard`, `maybeboard`).

### 6.3 Trigger critico

Funcion y trigger:

- `check_card_quantity()`
- `enforce_card_quantity_limit` en `card_deck` (`BEFORE INSERT OR UPDATE`)

Validaciones del trigger:

- Omite validacion dura para `maybeboard`.
- Verifica legalidad por formato (`card_legality`).
- Bloquea cartas `banned`.
- Aplica limite de copias segun reglas:
  - ilimitadas por bandera `unlimited_copies`
  - tierras basicas
  - `restricted` => max 1
  - formato `Commander` => max 1
  - default => max 4
- Aplica limite de tamano por seccion:
  - `sideboard` max 15
  - `main` segun `formats.deck_size`
- Actualiza `decks.last_modification`.

Implicacion:

- Cualquier logica de app sobre cantidad/legalidad debe mantenerse alineada con este trigger para evitar inconsistencias entre validacion en Java y validacion SQL.

## 7. Configuracion y ejecucion local

### 7.1 Properties de aplicacion

Archivo base:

- `src/main/resources/application.properties`

Datos relevantes:

- Puerto app: `${PORT:8080}`
- URL DB por defecto: `jdbc:postgresql://localhost:5433/mtg_db`
- Usuario por defecto: `user`
- Password: via `DB_PASSWORD`
- `spring.jpa.hibernate.ddl-auto=none`

### 7.2 Docker local

Archivo:

- `infra/docker-compose.yml`

Servicios:

- `db`: `ankane/pgvector`, puerto host `5433`
- `app`: expone `8080`, depende de `db`

Inicializacion de DB en Docker:

- monta `../src/main/resources/scripts/01-create-schema-ENHANCED.sql` en `/docker-entrypoint-initdb.d/01-create-schema.sql`

Implicacion:

- El flujo Docker usa el schema *ENHANCED*, mientras que en recursos existe tambien `01-create-schema.sql`. Antes de tocar schema, definir claramente cual es el source of truth operativo para tu entorno objetivo (local, CI, produccion).

## 8. Build, generacion y pruebas

Comandos esperados (usa comandos genericos de Maven para mantener esta guia independiente del sistema operativo):

```bash
mvn clean install
mvn test
mvn generate-sources
```

Plugins Maven relevantes:

- `scs-multiapi-maven-plugin` genera codigo OpenAPI hacia `target/generated-sources/apigenerator`
- `build-helper-maven-plugin` agrega ese source al compile path

Regla para agentes:

- No editar manualmente codigo generado bajo `target/generated-sources/...`.
- Si cambias `openapi.yml`, regenerar fuentes y validar compilacion/tests.

## 9. Cobertura de tests observada

### 9.1 Application

- `CardServiceImplTest`
- `CardTagServiceImplTest`
- `DeckServiceImplTest`
- `DeckValidationServiceTest`
- `FormatServiceImplTest`
- `SetServiceImplTest`
- `TagServiceImplTest`
- `UserServiceImplTest`

### 9.2 Contract

- `CardControllerTest`
- `DeckControllerTest`
- `FormatControllerTest`
- `SetControllerTest`
- `TagControllerTest`
- `UserControllerTest`

### 9.3 Infrastructure

- tests para repositorios (`CardRepositoryTest`, `DeckRepositoryTest`, etc.)

### 9.4 Integracion

- `DeckServiceH2IntegrationTest`
- `DeckValidationIntegrationTest`
- `FullWorkflowIntegrationTest`

## 10. Riesgos tecnicos y puntos de control

1. **Desalineacion OpenAPI vs implementacion:** cambios en contrato sin regeneracion.
2. **Inconsistencia naming de paginacion en endpoints:** `pageSize/pageNumber` vs `pagesize/pagenumber`.
3. **Reglas duplicadas entre Java y SQL trigger:** divergencias en cantidad/legality.
4. **Dos scripts de schema (base y enhanced):** riesgo de drift entre entornos.
5. **Embeddings/pgvector:** dependencias de extension y estructura vectorial deben existir en runtime.

## 11. Protocolo de trabajo recomendado para agentes

1. Leer primero:
   - `pom.xml`
   - `src/main/resources/api/openapi.yml`
   - `src/main/resources/scripts/01-create-schema.sql`
   - `infra/docker-compose.yml`
2. Localizar el caso de uso en `application` y su endpoint en `contract`.
3. Revisar repositorios impactados en `infrastructure`.
4. Implementar cambios minimos y coherentes por capa.
5. Ajustar pruebas unitarias/integracion relacionadas.
6. Si hay cambios de contrato, ejecutar regeneracion OpenAPI.
7. Verificar build/test antes de cerrar.

## 12. Checklist de QA para PRs

- [ ] Contrato OpenAPI consistente con controllers/DTOs.
- [ ] Reglas de cantidades/legalidad cubiertas en tests.
- [ ] `last_modification` se actualiza en operaciones de deck.
- [ ] No hay cambios manuales en codigo generado.
- [ ] Cambios SQL alineados entre scripts usados en runtime.

## 13. Preguntas abiertas para futuras iteraciones

- Definir y documentar claramente la estrategia de autenticacion/autorizacion.
- Consolidar un unico script de schema como fuente oficial (o documentar roles de cada uno).
- Estandarizar naming de parametros de paginacion en OpenAPI.
- Documentar politica de migraciones de DB (Flyway/Liquibase si aplica en futuro).

---

Si eres un agente nuevo, empieza por la seccion 11 y luego ejecuta el caso de uso objetivo con trazabilidad por capas (`contract -> application -> infrastructure -> SQL constraints`).



