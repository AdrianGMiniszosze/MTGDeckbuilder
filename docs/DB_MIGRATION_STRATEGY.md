# DB Migration Strategy

This document defines the migration policy for schema changes in `MTGDeckbuilder`.

## Decision (Sprint S1-01)

- **Chosen source of truth**: Flyway versioned migrations.
- **Current state**: existing SQL bootstrap scripts are still present for local bootstrap compatibility.
- **Transition rule**: from this point, schema evolution must be done with new Flyway migrations, not by editing historical migration files.

## Goals

1. Eliminate schema drift between runtime DB, SQL scripts, and JPA entities.
2. Make schema changes deterministic and reviewable in pull requests.
3. Support safe roll-forward with auditable migration history.

## Migration workflow

### 1) Authoring

1. Create a new migration file under `src/main/resources/db/migration/`.
2. Use sequential version naming (example: `V1__baseline_schema.sql`, `V2__add_scryfall_identity.sql`).
3. Keep each migration focused on a single concern.
4. Never modify an already merged migration file.

### 2) Local development

1. Start local infrastructure.
2. Run application/tests and let Flyway apply pending migrations.
3. If a migration fails locally, create a new corrective migration (do not rewrite merged files).

### 3) CI

1. CI runs tests against a clean database.
2. Flyway must apply all migrations successfully before integration tests proceed.
3. PRs that introduce schema changes must include corresponding migration files.

### 4) Deployment and rollback policy

- **Preferred strategy**: roll-forward.
- If a release fails after schema change, deploy a corrective migration in a new release.
- Avoid destructive down migrations in production paths.
- Backups/snapshots are operational safeguards, not a replacement for migration discipline.

## Data-safety rules

1. Never use card name as identity for dedupe/upsert logic.
2. Use `scryfall_id` as unique printing identifier.
3. Keep `oracle_id` non-unique and indexed.
4. For high-risk operations (mass updates, type changes), perform staged migrations.

## Review checklist for PRs with schema changes

- [ ] Migration file added in `src/main/resources/db/migration/`.
- [ ] Backward compatibility impact documented.
- [ ] Index/constraint implications reviewed.
- [ ] Related JPA/entity updates included (if required).
- [ ] Test coverage updated for new schema behavior.

## Ownership

- Schema migration decisions are tracked in `docs/ARCHITECTURE_DECISIONS.md`.
- Operational usage and developer workflow are described in this document.

