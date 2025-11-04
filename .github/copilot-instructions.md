# MTG Deckbuilder AI Instructions

This document guides AI agents working in the MTG Deckbuilder codebase. It provides essential context about the project's architecture, conventions, and workflows.

## Project Overview

MTG Deckbuilder is a Spring Boot application for managing Magic: The Gathering card collections and decks. The system uses:

- Java 21 with Spring Boot 3.5.6
- PostgreSQL with pgvector extension for card embeddings
- OpenAPI-driven API design with automatic code generation
- Docker Compose for local development

## Key Architecture Components

### Database Schema

The database uses PostgreSQL with pgvector extension for card embeddings. Key tables include:
- `cards` - Card information including vector embeddings
- `sets` - MTG card sets
- `users` - User accounts
- `decks` - User-created decks
- `formats` - Game formats (e.g., Standard, Commander)
- `tags` - Card tagging system

Important relationships:
- `card_deck` - Links cards to decks with quantity
- `card_legality` - Defines card legality in different formats
- `card_tag` - Associates tags with cards, includes ML confidence scores

### API Design

The API is defined in `src/main/resources/api/openapi.yml` and follows a REST pattern:
- `/cards` - Card management
- `/decks` - Deck CRUD operations
- `/users` - User management
- `/formats` - Format definitions
- `/sets` - Set management
- `/tags` - Card tagging system

Models are auto-generated using the scs-multiapi-maven-plugin with DTO suffix.

## Development Workflows

### Local Development Setup

1. Start the database and application:
```bash
cd infra
docker compose up -d
```

2. Application runs at http://localhost:8080/api

### Build and Test

Build the project:
```bash
./mvnw clean install
```

Run tests:
```bash
./mvnw test
```

### Code Generation

API models are generated from OpenAPI spec:
```bash
./mvnw generate-sources
```

## Project-Specific Conventions

### Database Conventions

1. All tables use explicit foreign key constraints with `ON DELETE CASCADE`
2. Card embedding vectors use PostgreSQL's vector type (1536 dimensions)
3. Tables use timestamps with timezone (`TIMESTAMP WITH TIME ZONE`)
4. Card quantities and deck validation use database triggers (`check_card_quantity`)

### API Conventions

1. All endpoints support pagination with `pageSize` and `pageNumber` parameters
2. Card-Tag relationships include ML metadata (confidence, source, model version)
3. Deck modifications automatically update `last_modification` timestamp
4. Each deck type (main, sideboard, maybeboard) has specific size constraints

## Integration Points

1. Database: Connects via JDBC to PostgreSQL (see `application.properties`)
2. Card Embeddings: Uses pgvector for embedding storage and similarity search
3. Authentication: (TODO: Document auth system when implemented)

## Common Operations

1. Adding cards to deck:
   - Check format legality
   - Verify quantity limits
   - Update deck modification time

2. Managing card legality:
   - Format-specific rules
   - Restricted/banned list handling
   - Basic land exceptions

Remember to check database triggers when modifying card quantities or deck contents, as they enforce game rules and deck construction constraints.