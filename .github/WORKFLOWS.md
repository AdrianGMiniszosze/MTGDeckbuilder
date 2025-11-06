# GitHub Actions Workflows

This repository includes several GitHub Actions workflows to automate various aspects of the development lifecycle.

## Available Workflows

### 1. CI Build and Test (`ci.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

**Purpose:**
Builds and tests the application with every code change. This workflow:
- Sets up Java 21 and Maven
- Starts a PostgreSQL database with pgvector extension
- Compiles the code
- Runs all unit and integration tests
- Uploads test results and build artifacts

**Database:** Uses `pgvector/pgvector:pg16` with credentials:
- Database: `mtgdeckbuilder`
- Username: `mtguser`
- Password: `mtgpass`

### 2. CodeQL Security Scan (`codeql.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Scheduled: Every Monday at 1:30 AM UTC

**Purpose:**
Performs static code analysis to identify security vulnerabilities and code quality issues using GitHub's CodeQL engine. The workflow uses:
- Security-extended queries
- Security and quality analysis

### 3. Dependency Review (`dependency-review.yml`)

**Triggers:**
- Pull requests to `main` or `develop` branches

**Purpose:**
Reviews dependencies added or modified in pull requests to identify:
- Known security vulnerabilities
- License issues
- Outdated dependencies

The workflow will fail if dependencies with moderate or higher severity vulnerabilities are introduced and posts a summary comment on the PR.

### 4. Docker Build and Publish (`docker.yml`)

**Triggers:**
- Push to `main` branch
- Pull requests to `main` branch
- Tags matching `v*.*.*` pattern

**Purpose:**
Builds a Docker image of the application and publishes it to GitHub Container Registry (ghcr.io). Features:
- Multi-stage build for optimized image size
- Uses Java 21
- Runs as non-root user for security
- Automatic tagging based on branch, PR, semver, and SHA
- Build cache optimization with GitHub Actions cache

**Image Tags:**
- Branch name (e.g., `main`)
- Pull request number (e.g., `pr-123`)
- Semantic version (e.g., `1.2.3`, `1.2`, `1`)
- Git SHA (e.g., `sha-abc1234`)

### 5. Stale Issues and PRs (`stale.yml`)

**Triggers:**
- Scheduled: Daily at 2:30 AM UTC
- Manual trigger via workflow_dispatch

**Purpose:**
Automatically manages stale issues and pull requests:
- Marks issues/PRs as stale after 60 days of inactivity
- Closes stale issues/PRs after 14 additional days
- Exempts items labeled with: `pinned`, `security`, or `enhancement`

## Dependabot Configuration

The repository uses Dependabot to automatically create pull requests for dependency updates:

### Maven Dependencies
- Runs weekly on Mondays at 6:00 AM UTC
- Up to 10 open PRs at a time
- Labels: `dependencies`, `java`

### GitHub Actions
- Runs weekly on Mondays at 6:00 AM UTC
- Up to 5 open PRs at a time
- Labels: `dependencies`, `github-actions`

### Docker Base Images
- Runs weekly on Mondays at 6:00 AM UTC
- Up to 5 open PRs at a time
- Labels: `dependencies`, `docker`

## Docker Support

### Building the Docker Image Locally

```bash
docker build -t mtg-deckbuilder .
```

### Running the Docker Container

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mtgdeckbuilder \
  -e SPRING_DATASOURCE_USERNAME=mtguser \
  -e SPRING_DATASOURCE_PASSWORD=mtgpass \
  mtg-deckbuilder
```

### Using with Docker Compose

The application is designed to work with the existing `docker-compose.yml` in the `infra/` directory. You can extend it to include the application:

```yaml
services:
  app:
    build: ..
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mtgdeckbuilder
      SPRING_DATASOURCE_USERNAME: mtguser
      SPRING_DATASOURCE_PASSWORD: mtgpass
    depends_on:
      - postgres
```

## Required Secrets

No additional secrets are required for these workflows. They use:
- `GITHUB_TOKEN` - Automatically provided by GitHub Actions

## Permissions

The workflows use the following permissions:

- **CI Build and Test**: Default read permissions
- **CodeQL**: `actions: read`, `contents: read`, `security-events: write`
- **Dependency Review**: `contents: read`, `pull-requests: write`
- **Docker**: `contents: read`, `packages: write`
- **Stale**: `issues: write`, `pull-requests: write`

## Workflow Status Badges

Add these badges to your README.md to show workflow status:

```markdown
![CI Build and Test](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/ci.yml/badge.svg)
![CodeQL](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/codeql.yml/badge.svg)
![Docker](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/docker.yml/badge.svg)
```

## Troubleshooting

### CI Tests Failing
- Ensure PostgreSQL connection settings are correct
- Check that all required environment variables are set
- Verify Java 21 compatibility of dependencies

### Docker Build Failing
- Ensure the Dockerfile is in the repository root
- Check that Maven dependencies can be resolved
- Verify the application builds successfully with `mvn clean package`

### CodeQL Scanning Issues
- CodeQL requires successful compilation
- If custom build steps are needed, modify the autobuild step
- Check CodeQL action documentation for language-specific requirements

## Best Practices

1. **Pull Requests**: Always create PRs to trigger the full suite of checks before merging
2. **Security**: Review Dependabot PRs promptly, especially for security updates
3. **Testing**: Ensure tests pass locally before pushing
4. **Docker Images**: Tag releases properly using semantic versioning (e.g., `v1.0.0`)
5. **Stale Items**: Label important issues with `pinned` to prevent automatic closure

## Contributing

When adding new workflows:
1. Test them in a feature branch first
2. Document the workflow purpose and triggers
3. Update this README with new workflow information
4. Ensure proper permissions are set
5. Add appropriate status badges
