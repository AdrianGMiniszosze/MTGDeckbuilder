# MTGDeckbuilder

A Spring Boot application for managing Magic: The Gathering card collections and decks.

## Features

- Card collection management
- Deck building with format validation
- PostgreSQL database with pgvector for card embeddings
- RESTful API with OpenAPI specification
- Docker support for easy deployment

## Technology Stack

- **Backend**: Spring Boot 3.5.6
- **Language**: Java 21
- **Database**: PostgreSQL with pgvector extension
- **Build Tool**: Maven
- **API Documentation**: OpenAPI 3.0

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose (for database)

### Running Locally

1. Start the PostgreSQL database:
   ```bash
   cd infra
   docker compose up -d
   ```

2. Build the application:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will be available at `http://localhost:8080/api`

### Running with Docker

Build and run the entire application in Docker:

```bash
docker build -t mtg-deckbuilder .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mtgdeckbuilder \
  -e SPRING_DATASOURCE_USERNAME=mtguser \
  -e SPRING_DATASOURCE_PASSWORD=mtgpass \
  mtg-deckbuilder
```

## CI/CD

This repository includes comprehensive GitHub Actions workflows for:

- **Continuous Integration**: Automated build and test on every PR
- **Security Scanning**: CodeQL analysis and dependency review
- **Docker Publishing**: Automated image builds and publishing to GHCR
- **Dependency Management**: Automated updates via Dependabot

See [.github/WORKFLOWS.md](.github/WORKFLOWS.md) for detailed workflow documentation.

### Workflow Status

![CI Build and Test](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/ci.yml/badge.svg)
![CodeQL](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/codeql.yml/badge.svg)
![Docker](https://github.com/AdrianGMiniszosze/MTGDeckbuilder/actions/workflows/docker.yml/badge.svg)

## API Documentation

The API is defined using OpenAPI specification at `src/main/resources/api/openapi.yml`.

## Development

### Project Structure

```
├── .github/
│   ├── workflows/          # GitHub Actions workflows
│   ├── dependabot.yml      # Dependabot configuration
│   └── WORKFLOWS.md        # Workflow documentation
├── infra/
│   └── docker-compose.yml  # PostgreSQL with pgvector
├── src/
│   ├── main/
│   │   ├── java/           # Application source code
│   │   └── resources/      # Configuration and OpenAPI spec
│   └── test/               # Test files
├── Dockerfile              # Multi-stage Docker build
└── pom.xml                # Maven configuration
```

### Running Tests

```bash
mvn test
```

### Building the Project

```bash
mvn clean package
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and ensure they pass
5. Submit a pull request

All pull requests will be automatically checked by CI workflows.

## License

[Add your license here]