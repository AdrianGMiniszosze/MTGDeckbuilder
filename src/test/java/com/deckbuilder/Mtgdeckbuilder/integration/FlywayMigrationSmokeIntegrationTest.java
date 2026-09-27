package com.deckbuilder.mtgdeckbuilder.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationSmokeIntegrationTest {

    @Test
    void appliesV2AfterBootstrapBaselineOnCleanDatabase() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                .withDatabaseName("mtg_test")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("scripts/01-create-schema-ENHANCED.sql")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();

            flyway.migrate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                assertThat(hasColumn(connection, "cards", "scryfall_id")).isTrue();
                assertThat(hasColumn(connection, "cards", "oracle_id")).isTrue();
                assertThat(hasColumn(connection, "cards", "imported_at")).isTrue();
                assertThat(hasColumn(connection, "cards", "updated_at")).isTrue();
                assertThat(hasTrigger(connection, "cards", "trg_cards_updated_at")).isTrue();
            }
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws Exception {
        String sql = """
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean hasTrigger(Connection connection, String table, String trigger) throws Exception {
        String sql = """
                SELECT 1
                FROM pg_trigger t
                JOIN pg_class c ON c.oid = t.tgrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relname = ?
                  AND t.tgname = ?
                  AND NOT t.tgisinternal
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, trigger);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}

