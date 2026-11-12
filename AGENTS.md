# AGENTS.md

Spring Boot REST API for Magic: The Gathering deck building. One Maven module, Java 21, Spring Boot 3.5.6. This file is verified against `pom.xml`, CI, and source; parts of `README.md` and `docs/*.md` are stale.

## Read this first

- Package root is `src/main/java/com/deckbuilder/mtgdeckbuilder` (all lowercase); main class `MtgdeckbuilderApplication`. The README's dual `Mtgdeckbuilder` / `mtgdeckbuilder` package tree is outdated — do not create a capitalized package.
- Layers: `contract` (controllers, DTO mappers) → `application` (services + business rules) → `infrastructure` (JPA entities, repositories, entity mappers) → `model` (domain). Both mapper layers are intentional: `contract/mapper` maps domain ↔ generated DTO, `infrastructure/mapper` maps domain ↔ entity.
- Keep business logic in `application`/`model`, persistence in `infrastructure`, controllers thin.
- `AGENT_QUICKSTART.md` and `TECHNICAL_AGENT_CONTEXT.md` are Spanish and partly outdated (they suggest bare `mvn`); prefer this file and executable config. `docs/DB_MIGRATION_STRATEGY.md` and `docs/ARCHITECTURE_DECISIONS.md` are still relevant for schema decisions.

## Commands (PowerShell, repo root)

Use the wrapper; a global Maven install is not required.

```powershell
# OpenAPI codegen must run before compiling code that uses generated *Api/*DTO classes
.\mvnw.cmd generate-sources
.\mvnw.cmd compile
.\mvnw.cmd clean package -DskipTests

# Unit tests (no Docker)
.\mvnw.cmd test "-Dtest=*Test,!*IntegrationTest,!CardLegalityRepositoryTest,!CardInDeckRepositoryExtendedTest" -DfailIfNoTests=false

# One unit test
.\mvnw.cmd test -Dtest=CardServiceImplTest -DfailIfNoTests=false

# Docker-backed tests (Docker Desktop must be running)
.\mvnw.cmd test -Dtest=FlywayMigrationSmokeIntegrationTest -DfailIfNoTests=false -DforkCount=1 -DreuseForks=true
.\mvnw.cmd test "-Dtest=*IntegrationTest,!FlywayMigrationSmokeIntegrationTest,CardLegalityRepositoryTest,CardInDeckRepositoryExtendedTest" -DfailIfNoTests=false -DforkCount=1 -DreuseForks=true
```

On macOS/Linux substitute `./mvnw`. There is no lint, formatter, or typecheck plugin; verification is codegen + compile + the relevant tests.

## API contract and generated code

- `src/main/resources/api/openapi.yml` is the source of truth. It generates interfaces (`CardsApi`, `DecksApi`, …) and `*DTO` models into `target/generated-sources/apigenerator`. Controllers implement those generated interfaces, so changing the spec changes controller method signatures/parameter names.
- Never edit `target/` or generated classes. After changing `openapi.yml`, run `generate-sources` and recompile; use `clean` if generated output looks stale.
- The spec's `servers: /api/v1` is **not applied** by the generator or config. Actual routes are root-level (`/cards`, `/decks`, …), despite the README's `/api/v1` examples.
- Pagination is deliberately inconsistent: `/sets` uses `pageSize`/`pageNumber`; every other endpoint uses `pagesize`/`pagenumber`. Match each endpoint's spec. Generated DTO fields can be snake_case (e.g. `page_size`, `card_name`); use the generated names.

## Database and schema

- PostgreSQL + pgvector. Local Compose DB is `localhost:5433/mtg_db`; app defaults to port 8080. `DB_PASSWORD` is required unless using the `dev` profile (default `changeme`).
- Flyway is the only schema source. `V1__baseline_schema.sql` creates the full schema and `V2__seed_formats.sql` seeds Standard, Commander, Modern, Legacy, Vintage and Pauper. Docker no longer mounts bootstrap scripts.
- New schema changes: add a focused `V*__*.sql` in `src/main/resources/db/migration/`. Migrations are immutable once applied; fix forward with a new migration. The V1 consolidation is the one-time exception that replaced the old bootstrap plus `V2`–`V5`.
- Use `scryfall_id` as the unique printing identity; `oracle_id` is non-unique and indexed. Never use card name for dedupe/upsert. Prices live on `cards`.
- `CREATE EXTENSION vector` runs in `V1`, but `cards.embedding`, the similarity index and `find_similar_cards` are deliberately deferred to `V3` until the embedding model and dimension are chosen. No Java semantic-search endpoint exists yet.
- Deck rules are enforced twice: Java (`DeckValidationService`, `CompanionRules`) and the SQL trigger `check_card_quantity` on `card_deck`. The trigger uses `formats.max_copies`/`singleton`/`max_sideboard_size`/`max_deck_size`, rejects `banned`/`not_legal`, exempts `is_basic_land`/`unlimited_copies`, counts copies across main + sideboard, ignores `maybeboard`, and updates `decks.updated_at`. Change both sides and add integration coverage.
- Entities are still being aligned to the V1 schema (`format_id`, `is_private`, `set_id`, `related_card_id`, `created_at`/`updated_at`). Until that lands, do not run the app against a freshly migrated database.
- Valid deck sections/types: `main`, `sideboard`, `maybeboard`.
- Runtime Hibernate is `ddl-auto=none`; entities must match Flyway-managed tables. The committed local-config template is `application.properties.example` — `src/main/resources/application.properties` is gitignored, so edit the example or profile files instead.

## Testing quirks

- Repositories/integration tests use Testcontainers image `pgvector/pgvector:pg16`, initialized by `src/test/resources/testcontainers-init.sql`. Extend `PostgresIntegrationTest` for `@SpringBootTest` or `PostgresDataJpaTest` for `@DataJpaTest`; do not add H2.
- The `testcontainers` profile sets Hibernate `ddl-auto=create-drop` and disables Flyway, so entity/schema drift is only caught by `FlywayMigrationSmokeIntegrationTest`, which starts an empty database, applies `V1` + `V2` with Flyway and asserts the canonical schema, seed rules and deck trigger. It no longer uses the removed bootstrap scripts.
- Naming drives CI suites: plain `*Test` runs in the no-Docker job; `*IntegrationTest` plus `CardLegalityRepositoryTest` and `CardInDeckRepositoryExtendedTest` need Docker. If you add another Testcontainers test not ending in `IntegrationTest`, update the filter lists in `README.md` and `.github/workflows/ci.yml`.
- `DeckServiceH2IntegrationTest` is a legacy name — it runs on PostgreSQL/Testcontainers. Ignore the H2 references in `application-test.properties`; H2 is not a dependency and no test uses the `test` profile.
- **Known issue — Docker Desktop 29.x + Testcontainers 1.21.x (mitigated):** `docker-java` 3.4.2 defaults to Docker API `1.32`, but Docker 29 rejects API versions below `1.40` with `400 Bad Request`, so Testcontainers fails with `Could not find a valid Docker environment` even though `docker info` works. CI detects the daemon version (`docker version --format '{{.Server.APIVersion}}'`) and passes `-Dapi.version=$DOCKER_API_VERSION` to Maven, so no version is hardcoded. Locally, pass the same property explicitly (for example `-Dapi.version=1.56`). Permanent fix: upgrade Testcontainers/docker-java to a version that negotiates the daemon API (latest on Maven Central at time of writing is Testcontainers `2.0.5`; verify before adopting).

## Git, PRs, CI

- Mandatory guardrails are in `.github/copilot-instructions.md`: never `git push`, open/edit/merge/close a PR, or use admin overrides without explicit approval in the current session. Default to local edits, tests, and commits; ask a direct yes/no confirmation before publishing, e.g. "I am ready to push. Proceed? (yes/no)".
- On PowerShell, write multi-line PR bodies to a file and use `gh pr create --body-file` / `gh pr edit --body-file`; do not embed literal `\n` in `--body`.
- `ci.yml` runs on PRs to `master`/`develop` and pushes to `master`: generate-sources → compile → unit tests → Flyway smoke test → integration tests. `build-check.yml` verifies the JAR packages. No Docker-free full-suite equivalent exists.

## Local-only files

`Summary and improvement.txt`, `plan-*.prompt.md`, `knowledge/`, `docs/scryfall_api_completa.md`, and the Scryfall `*.jsonl` files are gitignored local notes/artifacts. Do not treat them as canonical repo state.
