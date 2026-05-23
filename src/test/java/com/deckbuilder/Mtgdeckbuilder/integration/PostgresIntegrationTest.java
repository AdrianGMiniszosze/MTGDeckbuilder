package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.support.SharedPostgresContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that require a real PostgreSQL database.
 * Uses a shared Testcontainers PostgreSQL instance across all test slices.
 */
@SpringBootTest
@ActiveProfiles("testcontainers")
public abstract class PostgresIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        SharedPostgresContainer.registerProperties(registry);
    }
}
