# Production readiness

Phase 13 prepares deployable defaults and startup checks. It does **not** mean a
production deployment is approved: no hosting platform, public domain, SMTP
provider, backup target, or recovery objective has been selected.

## Production profile

The image defaults to `SPRING_PROFILES_ACTIVE=prod`. The local
`infra/docker-compose.yml` explicitly overrides this to `dev` and includes
Mailpit; it is a development stack, not a production deployment manifest.

The `prod` profile fails startup unless these are configured:

| Variable | Requirement |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Remote PostgreSQL with pgvector; not loopback |
| `JWT_SECRET` | Unique secret, at least 32 UTF-8 bytes |
| `MAIL_HOST`, `MAIL_FROM` | Real SMTP host and provider-verified sender |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Required when SMTP AUTH is enabled |
| `MAIL_STARTTLS` or `MAIL_SSL` | At least one secure transport enabled |
| `APP_PUBLIC_URL` | HTTPS origin for account-action links |
| `CORS_ALLOWED_ORIGINS` | Exact HTTPS browser origins; must include `APP_PUBLIC_URL`'s origin |

`BOOTSTRAP_ADMIN_ENABLED` defaults to false. If enabled for initial provisioning,
use a secret-manager value, create the first admin, then disable it and restart.
`application-prod.properties` disables Flyway auto-baselining; provision a clean
schema with `V1`–`V9`. Do not set `baseline-on-migrate=true` to make a failed
migration appear successful.

The production configuration validator also rejects loopback databases, Mailpit
or loopback SMTP, plaintext SMTP, wildcard/non-HTTPS CORS, disabled shared rate
limits, and auto-baselining.

## Network and browser security

- Terminate TLS at the ingress/load balancer and redirect HTTP to HTTPS there.
- `server.forward-headers-strategy=none`: forwarded headers are not trusted by
  the application. Ingress must strip client-supplied forwarding headers.
- Rate limits use the TCP peer address. Behind a proxy, all callers may appear as
  one peer; configure an edge rate limiter until a trusted-proxy design is
  approved. Never trust `X-Forwarded-For` directly.
- CORS accepts only the explicit HTTPS origins in `CORS_ALLOWED_ORIGINS`; no
  wildcard and no credentialed cookies.
- `GET /actuator/health` and readiness/liveness status are public, with details
  hidden. Other actuator endpoints, including metrics and Prometheus, require
  `ADMIN`; expose/scrape them only on a trusted network.
- The container uses a JRE and non-root `app` user. Do not publish Mailpit or use
  the development Compose stack in production.

## Secrets and email

- Store database, JWT, SMTP and bootstrap values in the deployment secret manager,
  not in `.env`, image layers, CI logs or command-line arguments.
- Generate a JWT secret with at least 32 bytes of entropy, for example
  `openssl rand -base64 48`. Rotate it deliberately: rotating invalidates all
  outstanding access JWTs.
- Use an SMTP provider with TLS and an authenticated, verified sender. The app
  does not bundle a production mail provider.
- Email action delivery failures are counted by `auth.email.delivery.failure`
  without email/token labels. Resend/reset requests remain generic `202`.

## Database backups and rollout

- Enable encrypted automated backups and point-in-time recovery according to the
  chosen RPO/RTO. Compose's `postgres_data` volume is **not** a backup.
- Store backups off-host with retention and access controls.
- Before launch, restore a recent backup into an isolated PostgreSQL/pgvector
  database and run the migration/schema validation tests against it.
- Deploy schema changes forward-only. `FlywayMigrationSmokeIntegrationTest`
  verifies clean installation; `SchemaValidationIntegrationTest` checks JPA
  mappings against migrations.
- Roll out one instance first so Flyway completes, then add replicas. Rate-limit
  buckets and account-action tokens are shared in PostgreSQL.

## Release gates

- [ ] Choose hosting/region, DNS, TLS termination and trusted ingress.
- [ ] Supply production DB, JWT, SMTP, frontend URL, and exact CORS origins.
- [ ] Provision an admin; disable bootstrap afterward.
- [ ] Choose backup retention/RPO/RTO and complete a restore rehearsal.
- [ ] Restrict actuator/Prometheus scraping to trusted monitoring.
- [ ] Confirm GitHub CI and CodeQL pass; master protection should require review
      and checks rather than bypass.
- [ ] Smoke-test register → verify → login → refresh → reset → login in staging.

Local verification before release:

```powershell
.\mvnw.cmd clean verify "-Dapi.version=1.56" "-DforkCount=1" "-DreuseForks=true"
docker build -f infra/Dockerfile -t mtgdeckbuilder:release .
```

**Current gate:** code/schema checks pass, but a production sign-off still needs
the deployment-specific items above, especially a real SMTP provider, public
frontend origin, trusted ingress/rate-limit strategy, and backup/restore target.
