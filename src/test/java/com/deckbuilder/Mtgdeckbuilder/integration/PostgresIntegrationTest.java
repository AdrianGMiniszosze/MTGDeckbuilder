package com.deckbuilder.mtgdeckbuilder.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require a real PostgreSQL database.
 * Uses Testcontainers to spin up a pgvector-enabled PostgreSQL container.
 * Hibernate creates the schema from entities (create-drop), so the test DB
 * always reflects the current entity definitions.
 * The container is shared across all subclasses (static @Container) so it
 * only starts once per test suite, keeping CI times reasonable.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")
public abstract class PostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("mtg_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}

