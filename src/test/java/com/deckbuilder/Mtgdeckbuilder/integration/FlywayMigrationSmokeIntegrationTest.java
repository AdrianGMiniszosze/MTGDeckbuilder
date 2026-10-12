package com.deckbuilder.mtgdeckbuilder.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayMigrationSmokeIntegrationTest {

    @Test
    void appliesV2ThroughV5AndPreservesExistingPricesAfterBootstrapBaseline() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                .withDatabaseName("mtg_test")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("scripts/01-create-schema-ENHANCED.sql")) {
            postgres.start();

            try (Connection bootstrapConnection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                // Seed pre-migration data to validate timestamp backfill on existing rows.
                insertCardBeforeV2(bootstrapConnection, "pre_v2_card");
            }

            Flyway v2Flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .target(MigrationVersion.fromVersion("2"))
                    .load();
            v2Flyway.migrate();

            assertThat(v2Flyway.info().applied())
                    .extracting(migration -> migration.getVersion().getVersion())
                    .contains("2");

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                assertThat(isNotNullable(connection, "cards", "imported_at")).isFalse();
                assertThat(isNotNullable(connection, "cards", "updated_at")).isFalse();
            }

            Flyway v4Flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .target(MigrationVersion.fromVersion("4"))
                    .load();
            v4Flyway.migrate();

            assertThat(v4Flyway.info().applied())
                    .extracting(migration -> migration.getVersion().getVersion())
                    .contains("3", "4");

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                insertCardPrice(connection, getCardIdByName(connection, "pre_v2_card"));
            }

            Flyway v5Flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            v5Flyway.migrate();

            assertThat(v5Flyway.info().applied())
                    .extracting(migration -> migration.getVersion().getVersion())
                    .contains("3", "4", "5");

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                assertCardTimestampSchema(connection);
                assertImportSchema(connection);

                long seededCardId = getCardIdByName(connection, "pre_v2_card");
                assertThat(readUpdatedAt(connection, seededCardId)).isNotNull();
                assertThat(readUsd(connection, seededCardId)).isEqualByComparingTo("1.25");
                assertThat(readPriceObservedAt(connection, seededCardId)).isNotNull();

                UUID sharedOracleId = UUID.randomUUID();
                long cardWithScryfall = assertScryfallIdentityBehavior(connection, sharedOracleId);
                assertNewSchemaBehavior(connection, cardWithScryfall, sharedOracleId);
                assertUpdatedAtChangesOnUpdate(connection, cardWithScryfall);
            }
        }
    }

    private static void assertCardTimestampSchema(Connection connection) throws Exception {
        assertThat(hasColumn(connection, "cards", "scryfall_id")).isTrue();
        assertThat(hasColumn(connection, "cards", "oracle_id")).isTrue();
        assertThat(hasColumn(connection, "cards", "imported_at")).isTrue();
        assertThat(hasColumn(connection, "cards", "updated_at")).isTrue();
        assertThat(hasTrigger(connection, "cards", "trg_cards_updated_at")).isTrue();
        assertThat(hasIndex(connection, "ux_cards_scryfall_id")).isTrue();
        assertThat(hasIndex(connection, "ix_cards_oracle_id")).isTrue();
        assertThat(hasIndex(connection, "ix_cards_updated_at")).isTrue();
        assertThat(hasDefault(connection, "cards", "imported_at", "CURRENT_TIMESTAMP")).isTrue();
        assertThat(hasDefault(connection, "cards", "updated_at", "CURRENT_TIMESTAMP")).isTrue();
        assertThat(isNotNullable(connection, "cards", "imported_at")).isTrue();
        assertThat(isNotNullable(connection, "cards", "updated_at")).isTrue();
        assertThat(countCardsWithMissingTimestamps(connection)).isZero();
    }

    private static void assertImportSchema(Connection connection) throws Exception {
        assertThat(hasTable(connection, "card_prices")).isFalse();
        assertThat(hasColumn(connection, "cards", "usd")).isTrue();
        assertThat(hasColumn(connection, "cards", "usd_foil")).isTrue();
        assertThat(hasColumn(connection, "cards", "price_observed_at")).isTrue();
        assertThat(hasColumn(connection, "card_faces", "face_index")).isTrue();
        assertThat(hasColumn(connection, "card_faces", "face_data")).isTrue();
        assertThat(hasColumn(connection, "card_rulings", "oracle_id")).isTrue();
        assertThat(hasIndex(connection, "ix_card_rulings_oracle_id")).isTrue();
    }

    private static long assertScryfallIdentityBehavior(Connection connection, UUID sharedOracleId) throws Exception {
        UUID duplicateScryfallId = UUID.randomUUID();
        long cardWithScryfall = insertCardAfterV2(connection, "scryfall_unique_1", duplicateScryfallId, null);
        assertThat(readImportedAt(connection, cardWithScryfall)).isNotNull();
        assertThat(readUpdatedAt(connection, cardWithScryfall)).isNotNull();
        assertThatThrownBy(() -> insertCardAfterV2(connection, "scryfall_unique_2", duplicateScryfallId, null))
                .isInstanceOf(SQLException.class);

        insertCardAfterV2(connection, "oracle_reprint_1", UUID.randomUUID(), sharedOracleId);
        insertCardAfterV2(connection, "oracle_reprint_2", UUID.randomUUID(), sharedOracleId);
        assertThat(countCardsWithOracleId(connection, sharedOracleId)).isEqualTo(2);
        return cardWithScryfall;
    }

    private static void assertNewSchemaBehavior(Connection connection, long cardId, UUID oracleId) throws Exception {
        updateCardPrice(connection, cardId);
        assertThat(readUsd(connection, cardId)).isEqualByComparingTo("1.25");
        assertThatThrownBy(() -> updateCardPriceToNegative(connection, cardId))
                .isInstanceOf(SQLException.class);
        insertCardFace(connection, cardId, 0, "Front");
        insertCardFace(connection, cardId, 1, "Back");
        assertThat(countRows(connection, "card_faces", "card_id", cardId)).isEqualTo(2);
        assertThatThrownBy(() -> insertCardFace(connection, cardId, 1, "Duplicate"))
                .isInstanceOf(SQLException.class);

        insertRuling(connection, oracleId, "This ruling applies to multiple printings.");
        assertThatThrownBy(() -> insertRuling(connection, oracleId, "This ruling applies to multiple printings."))
                .isInstanceOf(SQLException.class);
        insertRuling(connection, oracleId, "A second ruling for the same oracle identity.");
        assertThat(countRows(connection, "card_rulings", "oracle_id", oracleId)).isEqualTo(2);
    }

    private static void assertUpdatedAtChangesOnUpdate(Connection connection, long cardId) throws Exception {
        OffsetDateTime beforeUpdate = readUpdatedAt(connection, cardId);
        updateCardText(connection, cardId);
        OffsetDateTime afterUpdate = readUpdatedAt(connection, cardId);
        assertThat(afterUpdate).isAfter(beforeUpdate);
    }

    private static long insertCardBeforeV2(Connection connection, String cardName) throws Exception {
        String sql = """
                INSERT INTO cards(card_name, cmc, type_line, card_type, rarity, card_text, image_url, language)
                VALUES (?, 1, 'Creature - Test', 'Creature', 'common', 'text', 'https://img', 'en')
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cardName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long insertCardAfterV2(Connection connection, String cardName, UUID scryfallId, UUID oracleId) throws Exception {
        String sql = """
                INSERT INTO cards(card_name, cmc, type_line, card_type, rarity, card_text, image_url, language, scryfall_id, oracle_id)
                VALUES (?, 1, 'Creature - Test', 'Creature', 'common', 'text', 'https://img', 'en', ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cardName);
            statement.setObject(2, scryfallId);
            statement.setObject(3, oracleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long getCardIdByName(Connection connection, String cardName) throws Exception {
        String sql = "SELECT id FROM cards WHERE card_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cardName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long countCardsWithMissingTimestamps(Connection connection) throws Exception {
        String sql = "SELECT COUNT(*) FROM cards WHERE imported_at IS NULL OR updated_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static long countCardsWithOracleId(Connection connection, UUID oracleId) throws Exception {
        String sql = "SELECT COUNT(*) FROM cards WHERE oracle_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, oracleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void insertCardPrice(Connection connection, long cardId) throws Exception {
        String sql = "INSERT INTO card_prices(card_id, usd, price_observed_at) VALUES (?, 1.25, CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            statement.executeUpdate();
        }
    }

    private static void updateCardPrice(Connection connection, long cardId) throws Exception {
        String sql = "UPDATE cards SET usd = 1.25, price_observed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            statement.executeUpdate();
        }
    }

    private static void updateCardPriceToNegative(Connection connection, long cardId) throws Exception {
        String sql = "UPDATE cards SET usd = -1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            statement.executeUpdate();
        }
    }

    private static void insertCardFace(Connection connection, long cardId, int faceIndex, String name) throws Exception {
        String sql = "INSERT INTO card_faces(card_id, face_index, name, face_data) VALUES (?, ?, ?, '{\"name\":\"test\"}'::jsonb)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            statement.setInt(2, faceIndex);
            statement.setString(3, name);
            statement.executeUpdate();
        }
    }

    private static void insertRuling(Connection connection, UUID oracleId, String comment) throws Exception {
        String sql = "INSERT INTO card_rulings(oracle_id, published_at, comment, source) VALUES (?, DATE '2026-01-01', ?, 'wotc')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, oracleId);
            statement.setString(2, comment);
            statement.executeUpdate();
        }
    }

    private static long countRows(Connection connection, String table, String column, Object value) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static boolean hasTable(Connection connection, String table) throws Exception {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static java.math.BigDecimal readUsd(Connection connection, long cardId) throws Exception {
        String sql = "SELECT usd FROM cards WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    private static OffsetDateTime readPriceObservedAt(Connection connection, long cardId) throws Exception {
        String sql = "SELECT price_observed_at FROM cards WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject(1, OffsetDateTime.class);
            }
        }
    }

    private static OffsetDateTime readImportedAt(Connection connection, long cardId) throws Exception {
        String sql = "SELECT imported_at FROM cards WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject(1, OffsetDateTime.class);
            }
        }
    }

    private static OffsetDateTime readUpdatedAt(Connection connection, long cardId) throws Exception {
        String sql = "SELECT updated_at FROM cards WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject(1, OffsetDateTime.class);
            }
        }
    }

    private static void updateCardText(Connection connection, long cardId) throws Exception {
        String sql = "UPDATE cards SET card_text = card_text || ' updated' WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            statement.executeUpdate();
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

    private static boolean hasIndex(Connection connection, String indexName) throws Exception {
        String sql = """
                SELECT 1
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean hasDefault(Connection connection, String table, String column, String expectedSnippet) throws Exception {
        String sql = """
                SELECT column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                String defaultValue = resultSet.getString(1);
                return defaultValue != null && defaultValue.contains(expectedSnippet);
            }
        }
    }

    private static boolean isNotNullable(Connection connection, String table, String column) throws Exception {
        String sql = """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && "NO".equals(resultSet.getString(1));
            }
        }
    }
}


