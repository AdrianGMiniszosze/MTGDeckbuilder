package com.deckbuilder.mtgdeckbuilder.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Centralized PostgreSQL Testcontainer for all test slices.
 *
 * Using one shared container avoids datasource URL drift when different
 * base test classes are initialized in the same Maven test run.
 */
public final class SharedPostgresContainer {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("mtg_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("testcontainers-init.sql");

    private static boolean started;

    private SharedPostgresContainer() {
    }

    public static synchronized void start() {
        if (!started) {
            POSTGRES.start();
            started = true;
        }
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}

