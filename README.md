# MTG Deckbuilder

> A REST API backend for building and managing **Magic: The Gathering** decks — built with Java 21 and Spring Boot.

[![CI](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/ci.yml/badge.svg)](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-blue)

---

## What is this?

MTG Deckbuilder is a backend API that lets users:

- **Manage card collections** — search and browse Magic cards.
- **Build decks** — create decks with main, sideboard and maybeboard sections, following real MTG construction rules (copy limits, format legality, Commander restrictions).
- **Explore cards semantically** — the system stores vector embeddings for each card using the `pgvector` PostgreSQL extension, enabling similarity-based search.

All behaviour is exposed through a clean RESTful API designed contract-first with OpenAPI 3.

---

## Tech Stack

| Technology | Role |
|---|---|
| Java 21 | Primary language |
| Spring Boot 3.5.6 | Web framework |
| Spring Data JPA | Database access layer |
| PostgreSQL + pgvector | Database + vector embeddings |
| OpenAPI 3 | API contract (code generation) |
| JUnit 5 + Mockito + AssertJ | Unit testing |
| Testcontainers | Integration tests against a real PostgreSQL |
| Docker Compose | Local development environment |

---

## Prerequisites

Before you start, make sure you have the following installed:

| Tool | Version | Notes |
|---|---|---|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Latest | Required to run the database locally |
| [Java 21 JDK](https://adoptium.net/temurin/releases/?version=21) | 21 | The JDK, not just the JRE |

> **Maven is not required.** This project includes a Maven wrapper (`mvnw` / `mvnw.cmd`) so you do not need Maven installed globally.

### macOS / Linux — allow the wrapper to run

```bash
chmod +x mvnw
```

### Windows — no extra steps needed

Use `mvnw.cmd` wherever the examples below show `./mvnw`.

---

## Quick Start

There are two ways to run the application:

| Path | Best for |
|---|---|
| **[Option A]** DB in Docker + app locally | Day-to-day development (faster, IDE debugging, hot reload) |
| **[Option B]** Everything in Docker | Quick demo, smoke-testing a production-like setup |

---

### Option A — DB in Docker, app running locally *(recommended)*

#### 1. Set up environment variables

**macOS / Linux / Git Bash**
```bash
cp infra/.env.example infra/.env
```

**Windows (PowerShell)**
```powershell
Copy-Item infra\.env.example infra\.env
```

**Windows (Command Prompt)**
```cmd
copy infra\.env.example infra\.env
```

Then open `infra/.env` and set a value for `POSTGRES_PASSWORD`. For Option A, this is the only password you need in `infra/.env`.

#### 2. Start the database

```bash
cd infra
docker compose up -d db
```

This starts PostgreSQL (with `pgvector`) on **localhost:5433**. The schema is created automatically on first startup.

#### 3. Set the DB password for the app

The app reads `DB_PASSWORD` from the environment. If you keep the default `changeme` password used by the `dev` profile, you can skip this step. Otherwise, set `DB_PASSWORD` to match what you put in `infra/.env`:

**macOS / Linux**
```bash
export DB_PASSWORD=yourpassword
```

**Windows (PowerShell)**
```powershell
$env:DB_PASSWORD = "yourpassword"
```

**Windows (Command Prompt)**
```cmd
set DB_PASSWORD=yourpassword
```

#### 4. Run the application

**macOS / Linux**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Windows**
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The API is now available at **http://localhost:8080/api/v1**.

---

### Option B — Everything in Docker

#### 1. Set up environment variables

**macOS / Linux / Git Bash**
```bash
cp infra/.env.example infra/.env
```

**Windows (PowerShell)**
```powershell
Copy-Item infra\.env.example infra\.env
```

**Windows (Command Prompt)**
```cmd
copy infra\.env.example infra\.env
```

Then open `infra/.env` and set `POSTGRES_PASSWORD`. When running the app container too (Option B), its datasource password must match the database password.

#### 2. Build the JAR first

**macOS / Linux**
```bash
./mvnw clean package -DskipTests
```

**Windows**
```cmd
mvnw.cmd clean package -DskipTests
```

#### 3. Start all services

```bash
cd infra
docker compose up -d
```

Both the database and the app will start. The API is available at **http://localhost:8080/api/v1**.

To stop everything:

```bash
docker compose down
```

To also remove the database volume (deletes all data):

```bash
docker compose down -v
```

---

## Running Tests

### Unit tests *(fast, no Docker required)*

**macOS / Linux**
```bash
./mvnw test \
  "-Dtest=*Test,!*IntegrationTest,!CardLegalityRepositoryTest,!CardInDeckRepositoryExtendedTest" \
  -DfailIfNoTests=false
```

**Windows**
```cmd
mvnw.cmd test "-Dtest=*Test,!*IntegrationTest,!CardLegalityRepositoryTest,!CardInDeckRepositoryExtendedTest" -DfailIfNoTests=false
```

### Integration tests *(require Docker to be running)*

Testcontainers automatically spins up a temporary PostgreSQL instance — no manual setup needed as long as Docker is running.

**macOS / Linux**
```bash
./mvnw test \
  "-Dtest=*IntegrationTest,CardLegalityRepositoryTest,CardInDeckRepositoryExtendedTest" \
  -DfailIfNoTests=false
```

**Windows**
```cmd
mvnw.cmd test "-Dtest=*IntegrationTest,CardLegalityRepositoryTest,CardInDeckRepositoryExtendedTest" -DfailIfNoTests=false
```

---

## API Overview

The full API contract is defined in `src/main/resources/api/openapi.yml`.

| Endpoint | Description |
|---|---|
| `GET /cards` | List and search cards |
| `GET /cards/{id}` | Get a single card |
| `GET /decks` | List decks |
| `POST /decks` | Create a new deck |
| `PUT /decks/{id}` | Update a deck |
| `DELETE /decks/{id}` | Delete a deck |
| `GET /users` | List users |
| `GET /users/{id}/decks` | List decks for a specific user |
| `GET /formats` | List game formats |
| `GET /sets` | List card sets |
| `GET /tags` | List tags |
| `POST /tags` | Create a tag |

All list endpoints support pagination, but parameter names are not fully standardized in the current contract: some endpoints use `pageSize` / `pageNumber`, while others use `pagesize` / `pagenumber`. Check `src/main/resources/api/openapi.yml` per endpoint until the contract is unified.

---

## Configuration Reference

The application resolves configuration from environment variables, with sensible defaults for local development.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/mtg_db` | JDBC URL of the database |
| `DB_USERNAME` | `user` | Database username |
| `DB_PASSWORD` | `changeme` (dev profile default) | Required outside the `dev` profile, or whenever your local DB password differs from the dev default |
| `PORT` | `8080` | HTTP server port |

See `src/main/resources/application.properties.example` and the profile-specific `src/main/resources/application-*.properties` files for the full property list, and `infra/.env.example` for the Docker Compose variables.

---

## Project Structure

```text
MTGDeckbuilder/
├── infra/                          # Docker Compose, Dockerfile, .env.example
├── src/
│   ├── main/
│   │   ├── java/com/deckbuilder/
│   │   │   ├── Mtgdeckbuilder/     # Primary package tree currently used by most code
│   │   │   └── mtgdeckbuilder/     # Additional package tree also present in this repo
│   │   └── resources/
│   │       ├── api/openapi.yml     # API contract (source of truth)
│   │       ├── scripts/            # SQL schema
│   │       └── application*.properties
│   └── test/
│       └── java/com/deckbuilder/Mtgdeckbuilder/  # Test packages
├── docs/                           # Architecture decisions & technical context
└── .github/workflows/              # CI/CD with GitHub Actions
```

> **Note:** This repository currently contains mixed-case package paths (`Mtgdeckbuilder` and `mtgdeckbuilder`). On case-insensitive filesystems this can be confusing and make renames/error diagnosis harder, so use the exact on-disk casing when navigating or creating files.

> **Important:** Never manually edit files inside `target/generated-sources/` — they are auto-generated from `openapi.yml` on every build.

---

## Database Notes

- The `cards.embedding` column is of type `VECTOR(1536)` and requires the `pgvector` extension, which is pre-installed in the Docker image used by this project.
- Deck construction rules (4-copy limit, Commander restrictions, format legality) are enforced by a SQL trigger (`check_card_quantity`) in the database itself.
- The `decks.last_modification` timestamp is updated automatically on any deck change.
- Valid card sections in a deck: `main`, `sideboard`, `maybeboard`.

---

## Regenerating Code from the OpenAPI Contract

If you modify `src/main/resources/api/openapi.yml`, regenerate the Java models by running:

**macOS / Linux**
```bash
./mvnw generate-sources
```

**Windows**
```cmd
mvnw.cmd generate-sources
```

---

## CI / CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | PRs to `master` and `develop`; pushes to `master` | Codegen → compile → unit tests → integration tests |
| `build-check.yml` | PRs to `master` and `develop` | Verifies the JAR packages correctly |

Integration tests run on `ubuntu-latest` where Docker is natively available and Testcontainers works with zero extra configuration.

---

## Contributing

1. Follow the layered architecture: `contract` → `application` → `infrastructure`.
2. Never edit code inside `target/generated-sources/` — it is regenerated automatically.
3. After changing the OpenAPI contract, run `generate-sources` and verify the build compiles cleanly.
4. All CI checks must pass before a PR can be merged.

---

## Troubleshooting

**`DB_PASSWORD` error on startup**  
When running with the `dev` profile, `spring.datasource.password` defaults to `changeme`, so you only need to set `DB_PASSWORD` if your local database password is different or if you are running without the `dev` profile. If you do need it, make sure it is exported in your current shell session (see Step 3 of Option A above).

**Port 5433 already in use**  
Another PostgreSQL instance may be running on your machine. Stop it, or change the host port mapping in `infra/docker-compose.yml` and update `DB_URL` to match.

**`Permission denied` running `./mvnw` on macOS / Linux**  
Run `chmod +x mvnw` once from the project root.

**Integration tests failing on Windows**  
Testcontainers requires Docker Desktop to be running. Prefer Docker Desktop's default engine socket with WSL2 integration enabled. Avoid exposing the daemon on unauthenticated TCP (`tcp://localhost:2375` without TLS). If needed, configure `DOCKER_HOST` to a secure local engine endpoint supported by your Docker Desktop setup.

---

## Further Documentation

- Architecture & design decisions: [`docs/ARCHITECTURE_DECISIONS.md`](docs/ARCHITECTURE_DECISIONS.md)
- Detailed technical context: [`docs/TECHNICAL_AGENT_CONTEXT.md`](docs/TECHNICAL_AGENT_CONTEXT.md)
- Agent onboarding guide: [`docs/AGENT_QUICKSTART.md`](docs/AGENT_QUICKSTART.md)
