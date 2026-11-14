package com.deckbuilder.mtgdeckbuilder.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that a completely empty database can be provisioned by Flyway alone
 * (V1 baseline + V2 format seed) and that the resulting schema enforces the
 * core deck rules.
 */
class FlywayMigrationSmokeIntegrationTest {

    @Test
    void appliesBaselineAndSeedToAnEmptyDatabase() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                .withDatabaseName("mtg_test")
                .withUsername("test")
                .withPassword("test")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            var result = flyway.migrate();

            assertThat(result.migrationsExecuted).isEqualTo(2);
            assertThat(flyway.info().applied())
                    .extracting(migration -> migration.getVersion().getVersion())
                    .containsExactly("1", "2");

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                assertCoreTablesExist(connection);
                assertCanonicalColumns(connection);
                assertLegacyColumnsAreGone(connection);
                assertSeedFormats(connection);
                assertDeckRulesTrigger(connection);
            }
        }
    }

    private static void assertCoreTablesExist(Connection connection) throws Exception {
        for (String table : new String[]{
                "users", "sets", "cards", "card_colors", "card_color_identity",
                "card_types", "card_supertypes", "card_subtypes", "card_keywords",
                "card_faces", "card_rulings", "formats", "card_legality", "decks",
                "card_deck", "tags", "card_tag", "card_relationships"}) {
            assertThat(hasTable(connection, table)).as("table %s", table).isTrue();
        }
    }

    private static void assertCanonicalColumns(Connection connection) throws Exception {
        assertThat(hasColumn(connection, "cards", "set_id")).isTrue();
        assertThat(hasColumn(connection, "cards", "related_card_id")).isTrue();
        assertThat(hasColumn(connection, "cards", "is_basic_land")).isTrue();
        assertThat(hasColumn(connection, "cards", "scryfall_id")).isTrue();
        assertThat(hasColumn(connection, "cards", "oracle_id")).isTrue();

        assertThat(hasColumn(connection, "decks", "format_id")).isTrue();
        assertThat(hasColumn(connection, "decks", "is_private")).isTrue();
        assertThat(hasColumn(connection, "decks", "created_at")).isTrue();
        assertThat(hasColumn(connection, "decks", "updated_at")).isTrue();

        assertThat(hasColumn(connection, "formats", "max_copies")).isTrue();
        assertThat(hasColumn(connection, "formats", "singleton")).isTrue();
        assertThat(hasColumn(connection, "formats", "requires_commander")).isTrue();
        assertThat(hasColumn(connection, "formats", "max_sideboard_size")).isTrue();

        assertThat(hasColumn(connection, "card_legality", "format_id")).isTrue();
        assertThat(hasColumn(connection, "users", "role")).isTrue();
        assertThat(hasColumn(connection, "users", "is_active")).isTrue();
    }

    private static void assertLegacyColumnsAreGone(Connection connection) throws Exception {
        assertThat(hasColumn(connection, "decks", "deck_type")).isFalse();
        assertThat(hasColumn(connection, "decks", "private")).isFalse();
        assertThat(hasColumn(connection, "cards", "card_set")).isFalse();
        assertThat(hasColumn(connection, "cards", "embedding")).isFalse();
    }

    private static void assertSeedFormats(Connection connection) throws Exception {
        assertThat(countFormats(connection)).isEqualTo(6);

        FormatRules standard = readFormat(connection, "Standard");
        assertThat(standard.minDeckSize()).isEqualTo(60);
        assertThat(standard.maxDeckSize()).isNull();
        assertThat(standard.maxSideboardSize()).isEqualTo(15);
        assertThat(standard.maxCopies()).isEqualTo(4);
        assertThat(standard.singleton()).isFalse();
        assertThat(standard.requiresCommander()).isFalse();

        FormatRules commander = readFormat(connection, "Commander");
        assertThat(commander.minDeckSize()).isEqualTo(100);
        assertThat(commander.maxDeckSize()).isEqualTo(100);
        assertThat(commander.maxCopies()).isEqualTo(1);
        assertThat(commander.singleton()).isTrue();
        assertThat(commander.requiresCommander()).isTrue();

        for (String name : new String[]{"Modern", "Legacy", "Vintage", "Pauper"}) {
            FormatRules rules = readFormat(connection, name);
            assertThat(rules.minDeckSize()).as(name).isEqualTo(60);
            assertThat(rules.maxCopies()).as(name).isEqualTo(4);
        }
    }

    private static void assertDeckRulesTrigger(Connection connection) throws Exception {
        assertThat(hasTrigger(connection, "card_deck", "enforce_card_quantity_limit")).isTrue();

        long userId = insertUser(connection);
        long standardFormatId = formatId(connection, "Standard");
        long deckId = insertDeck(connection, "Smoke Deck", standardFormatId, userId);

        long legalCard = insertCard(connection, "Legal Card", false, false);
        long bannedCard = insertCard(connection, "Banned Card", false, false);
        long basicLand = insertCard(connection, "Plains", true, false);

        insertLegality(connection, legalCard, standardFormatId, "legal");
        insertLegality(connection, bannedCard, standardFormatId, "banned");

        // Four copies are accepted.
        insertDeckCard(connection, deckId, legalCard, 4, "main");

        // A fifth copy is rejected.
        assertThatThrownBy(() -> insertDeckCard(connection, deckId, legalCard, 1, "sideboard"))
                .isInstanceOf(SQLException.class);

        // Banned cards are rejected.
        assertThatThrownBy(() -> insertDeckCard(connection, deckId, bannedCard, 1, "main"))
                .isInstanceOf(SQLException.class);

        // Basic lands are exempt from the copy limit.
        insertDeckCard(connection, deckId, basicLand, 20, "main");

        // Maybeboard is a scratch area.
        insertDeckCard(connection, deckId, bannedCard, 10, "maybeboard");
    }

    private static long insertUser(Connection connection) throws Exception {
        String sql = """
                INSERT INTO users(name, username, email, hashed_password)
                VALUES ('Smoke', 'smoke', 'smoke@example.com', 'hash')
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static long formatId(Connection connection, String formatName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM formats WHERE format_name = ?")) {
            statement.setString(1, formatName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long insertDeck(Connection connection, String name, long formatId, long userId) throws Exception {
        String sql = """
                INSERT INTO decks(deck_name, format_id, user_id)
                VALUES (?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setLong(2, formatId);
            statement.setLong(3, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long insertCard(Connection connection, String name, boolean basicLand, boolean unlimited) throws Exception {
        String sql = """
                INSERT INTO cards(card_name, cmc, type_line, card_type, rarity, image_url, is_basic_land, unlimited_copies)
                VALUES (?, 1, 'Creature - Test', 'Creature', 'common', 'https://img', ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setBoolean(2, basicLand);
            statement.setBoolean(3, unlimited);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void insertLegality(Connection connection, long cardId, long formatId, String status) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO card_legality(card_id, format_id, legality_status) VALUES (?, ?, ?)")) {
            statement.setLong(1, cardId);
            statement.setLong(2, formatId);
            statement.setString(3, status);
            statement.executeUpdate();
        }
    }

    private static void insertDeckCard(Connection connection, long deckId, long cardId, int quantity, String section) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO card_deck(deck_id, card_id, quantity, section) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, deckId);
            statement.setLong(2, cardId);
            statement.setInt(3, quantity);
            statement.setString(4, section);
            statement.executeUpdate();
        }
    }

    private static long countFormats(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM formats");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static FormatRules readFormat(Connection connection, String formatName) throws Exception {
        String sql = """
                SELECT min_deck_size, max_deck_size, max_sideboard_size, max_copies, singleton, requires_commander
                FROM formats
                WHERE format_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, formatName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("format %s exists", formatName).isTrue();
                Integer maxDeckSize = resultSet.getObject("max_deck_size", Integer.class);
                return new FormatRules(
                        resultSet.getInt("min_deck_size"),
                        maxDeckSize,
                        resultSet.getObject("max_sideboard_size", Integer.class),
                        resultSet.getInt("max_copies"),
                        resultSet.getBoolean("singleton"),
                        resultSet.getBoolean("requires_commander"));
            }
        }
    }

    private record FormatRules(int minDeckSize, Integer maxDeckSize, Integer maxSideboardSize,
                               int maxCopies, boolean singleton, boolean requiresCommander) {
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
