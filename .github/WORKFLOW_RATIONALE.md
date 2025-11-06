# Workflows That Fit This Repository

This document explains what GitHub Actions workflows are appropriate for the MTG Deckbuilder repository and the rationale behind each choice.

## Repository Context

MTG Deckbuilder is a Spring Boot application with:
- Java 21 backend
- Maven build system
- PostgreSQL database with pgvector extension
- OpenAPI-defined REST API
- Unit and integration tests
- Docker containerization support

## Recommended Workflows

### 1. ✅ CI Build and Test (`ci.yml`)

**Why It Fits:**
- Essential for any software project to ensure code quality
- Validates that every commit can be built successfully
- Runs the comprehensive test suite (223+ tests)
- Catches integration issues with PostgreSQL early
- Provides quick feedback to developers on PRs

**Key Features:**
- Runs on every push and PR to main/develop branches
- Sets up PostgreSQL with pgvector for integration tests
- Uploads test results and build artifacts
- Uses test reporter for clear failure visibility

### 2. ✅ CodeQL Security Scan (`codeql.yml`)

**Why It Fits:**
- Web applications handling user data need security scanning
- Identifies potential security vulnerabilities in Java code
- Catches common coding mistakes that could lead to exploits
- Runs automatically on schedule and with code changes
- GitHub's native security tool with zero configuration needed

**Key Features:**
- Scans Java codebase for security vulnerabilities
- Uses security-extended and quality queries
- Runs weekly and on every code change
- Integrates with GitHub Security tab

### 3. ✅ Dependency Review (`dependency-review.yml`)

**Why It Fits:**
- Maven projects have many transitive dependencies
- Spring Boot applications pull in numerous libraries
- Dependencies can have known security vulnerabilities
- Prevents introducing vulnerable dependencies in PRs

**Key Features:**
- Reviews all dependency changes in PRs
- Fails on moderate or higher severity issues
- Comments summary directly on PRs
- Helps maintain secure dependency tree

### 4. ✅ Docker Build and Publish (`docker.yml`)

**Why It Fits:**
- Application already uses Docker Compose for development
- Containerization is standard for Spring Boot deployments
- Enables consistent deployment across environments
- GHCR provides free container registry for GitHub projects

**Key Features:**
- Builds optimized multi-stage Docker images
- Publishes to GitHub Container Registry
- Tags images with branch, semver, and SHA
- Uses build cache for faster builds
- Runs as non-root user for security

### 5. ✅ Stale Issues and PRs (`stale.yml`)

**Why It Fits:**
- Helps maintain a clean issue tracker
- Prevents issues and PRs from being forgotten
- Reduces noise for maintainers
- Encourages active community participation

**Key Features:**
- Marks items stale after 60 days
- Closes after 14 additional days
- Exempts important labels (pinned, security, enhancement)
- Runs daily with clear messaging

### 6. ✅ Release Management (`release.yml`)

**Why It Fits:**
- Automates the release process
- Creates proper GitHub releases with artifacts
- Links releases to Docker images
- Provides changelog generation

**Key Features:**
- Triggered by version tags (e.g., v1.0.0)
- Builds and attaches JAR files
- Generates release notes
- Marks pre-releases appropriately

### 7. ✅ Dependabot Configuration (`dependabot.yml`)

**Why It Fits:**
- Maven dependencies need regular updates
- Spring Boot and Java ecosystem evolve rapidly
- Security patches are released frequently
- Manual dependency updates are error-prone

**Key Features:**
- Updates Maven dependencies weekly
- Updates GitHub Actions weekly
- Updates Docker base images weekly
- Creates automated PRs with changelogs

## Workflows NOT Included (and Why)

### ❌ Deploy to Production
**Reason:** No production environment is defined yet. Once deployment targets are established (e.g., AWS, Azure, Kubernetes), deployment workflows can be added.

### ❌ Performance Testing
**Reason:** No performance test suite exists. Once performance benchmarks are established, a workflow could be added to run them.

### ❌ Integration with External APIs
**Reason:** The application doesn't yet integrate with external MTG card APIs (like Scryfall). When external integrations are added, contract testing workflows would be valuable.

### ❌ Frontend Workflows
**Reason:** This is a backend-only application. If a frontend is added (React, Angular, etc.), separate workflows for frontend testing, linting, and building would be needed.

### ❌ Database Migrations
**Reason:** No automated database migration system is configured. If Flyway or Liquibase is added, migration testing workflows would be beneficial.

## Workflow Priority

For a new project, implement in this order:

1. **CI Build and Test** (Highest Priority)
   - Foundational workflow
   - Prevents broken code from being merged

2. **CodeQL Security Scan** (High Priority)
   - Security is critical for web applications
   - Easy to set up and maintain

3. **Dependabot** (High Priority)
   - Keeps dependencies secure and up-to-date
   - Minimal maintenance overhead

4. **Docker Build** (Medium Priority)
   - Enables deployment
   - Provides consistent environments

5. **Dependency Review** (Medium Priority)
   - Complements Dependabot
   - Catches issues in PRs

6. **Release Management** (Medium Priority)
   - Important for production releases
   - Can be manual initially

7. **Stale Management** (Low Priority)
   - Nice to have for project hygiene
   - More important as project matures

## Future Workflow Considerations

As the project evolves, consider adding:

1. **E2E Testing**: When UI is added
2. **Load Testing**: For performance validation
3. **Automated Deployment**: When production environment exists
4. **Notification Workflows**: For Slack/Discord/email alerts
5. **Backup Workflows**: For database backups
6. **Documentation Generation**: For API docs or JavaDocs
7. **License Compliance**: For OSS license checking

## Best Practices Applied

All workflows follow these best practices:

✅ **Explicit permissions**: Principle of least privilege
✅ **Caching**: Maven dependencies cached for speed
✅ **Version pinning**: Actions pinned to major versions
✅ **Security scanning**: CodeQL and dependency review
✅ **Documentation**: Comprehensive README and WORKFLOWS.md
✅ **Status badges**: Visible CI status in README
✅ **Automated updates**: Dependabot for all package ecosystems

## Conclusion

The workflows chosen for this repository provide:
- **Quality Assurance**: CI testing catches bugs early
- **Security**: Multiple layers of security scanning
- **Automation**: Reduces manual work for releases and updates
- **Visibility**: Clear status and notifications
- **Best Practices**: Industry-standard DevOps practices

These workflows create a solid foundation for professional software development while being appropriate for the project's current stage and technology stack.
