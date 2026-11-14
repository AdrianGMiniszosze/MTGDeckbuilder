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
 * (V1 baseline + V2 format seed + V3 deck validation model) and that the
 * resulting schema keeps data shape while format policy lives in Java.
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

            assertThat(result.migrationsExecuted).isEqualTo(3);
            assertThat(flyway.info().applied())
                    .extracting(migration -> migration.getVersion().getVersion())
                    .containsExactly("1", "2", "3");

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword())) {
                assertCoreTablesExist(connection);
                assertCanonicalColumns(connection);
                assertLegacyColumnsAreGone(connection);
                assertSeedFormats(connection);

                long userId = insertUser(connection);
                long standardFormatId = formatId(connection, "Standard");

                assertDeckStateConstraints(connection, standardFormatId, userId);
                assertCardDeckIntegrity(connection, standardFormatId, userId);
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
        assertThat(hasColumn(connection, "cards", "copies_limit")).isTrue();

        assertThat(hasColumn(connection, "decks", "format_id")).isTrue();
        assertThat(hasColumn(connection, "decks", "is_private")).isTrue();
        assertThat(hasColumn(connection, "decks", "created_at")).isTrue();
        assertThat(hasColumn(connection, "decks", "updated_at")).isTrue();
        assertThat(hasColumn(connection, "decks", "status")).isTrue();
        assertThat(hasColumn(connection, "decks", "published_at")).isTrue();

        assertThat(hasColumn(connection, "formats", "max_copies")).isTrue();
        assertThat(hasColumn(connection, "formats", "singleton")).isTrue();
        assertThat(hasColumn(connection, "formats", "requires_commander")).isTrue();
        assertThat(hasColumn(connection, "formats", "max_sideboard_size")).isTrue();
        assertThat(hasColumn(connection, "formats", "tracks_legality")).isTrue();

        assertThat(hasColumn(connection, "card_legality", "format_id")).isTrue();
        assertThat(hasColumn(connection, "users", "role")).isTrue();
        assertThat(hasColumn(connection, "users", "is_active")).isTrue();
    }

    private static void assertLegacyColumnsAreGone(Connection connection) throws Exception {
        assertThat(hasColumn(connection, "decks", "deck_type")).isFalse();
        assertThat(hasColumn(connection, "decks", "private")).isFalse();
        assertThat(hasColumn(connection, "cards", "card_set")).isFalse();
        assertThat(hasColumn(connection, "cards", "embedding")).isFalse();
        assertThat(hasColumn(connection, "cards", "unlimited_copies")).isFalse();
    }

    private static void assertSeedFormats(Connection connection) throws Exception {
        assertThat(countFormats(connection)).isEqualTo(7);

        FormatRules standard = readFormat(connection, "Standard");
        assertThat(standard.minDeckSize()).isEqualTo(60);
        assertThat(standard.maxDeckSize()).isNull();
        assertThat(standard.maxSideboardSize()).isEqualTo(15);
        assertThat(standard.maxCopies()).isEqualTo(4);
        assertThat(standard.singleton()).isFalse();
        assertThat(standard.requiresCommander()).isFalse();
        assertThat(standard.tracksLegality()).isTrue();

        FormatRules commander = readFormat(connection, "Commander");
        assertThat(commander.minDeckSize()).isEqualTo(100);
        assertThat(commander.maxDeckSize()).isEqualTo(100);
        assertThat(commander.maxCopies()).isEqualTo(1);
        assertThat(commander.singleton()).isTrue();
        assertThat(commander.requiresCommander()).isTrue();
        assertThat(commander.tracksLegality()).isTrue();

        FormatRules casual = readFormat(connection, "Casual");
        assertThat(casual.minDeckSize()).isZero();
        assertThat(casual.maxDeckSize()).isNull();
        assertThat(casual.maxSideboardSize()).isNull();
        assertThat(casual.maxCopies()).isEqualTo(4);
        assertThat(casual.singleton()).isFalse();
        assertThat(casual.requiresCommander()).isFalse();
        assertThat(casual.tracksLegality()).isFalse();

        for (String name : new String[]{"Modern", "Legacy", "Vintage", "Pauper"}) {
            FormatRules rules = readFormat(connection, name);
            assertThat(rules.minDeckSize()).as(name).isEqualTo(60);
            assertThat(rules.maxCopies()).as(name).isEqualTo(4);
            assertThat(rules.tracksLegality()).as(name).isTrue();
        }
    }

    private static void assertDeckStateConstraints(Connection connection, long formatId, long userId)
            throws Exception {
        long deckId = insertDeck(connection, "Draft Deck", formatId, userId);

        // New decks are drafts and have no publication timestamp.
        assertThat(readString(connection, "SELECT status FROM decks WHERE id = " + deckId)).isEqualTo("draft");
        assertThat(readString(connection, "SELECT published_at::text FROM decks WHERE id = " + deckId)).isNull();

        // Only the two known states are accepted.
        assertThatThrownBy(() -> execute(connection,
                "UPDATE decks SET status = 'archived' WHERE id = " + deckId))
                .isInstanceOf(SQLException.class);

        // Published requires a timestamp, and the timestamp requires published.
        assertThatThrownBy(() -> execute(connection,
                "UPDATE decks SET status = 'published' WHERE id = " + deckId))
                .isInstanceOf(SQLException.class);

        execute(connection, "UPDATE decks SET status = 'published', published_at = now() WHERE id = " + deckId);
        assertThat(readString(connection, "SELECT status FROM decks WHERE id = " + deckId)).isEqualTo("published");

        assertThatThrownBy(() -> execute(connection,
                "UPDATE decks SET published_at = NULL WHERE id = " + deckId))
                .isInstanceOf(SQLException.class);
    }

    private static void assertCardDeckIntegrity(Connection connection, long formatId, long userId)
            throws Exception {
        // Format policy no longer lives in the database.
        assertThat(hasTrigger(connection, "card_deck", "enforce_card_quantity_limit")).isFalse();
        assertThat(hasFunction(connection, "check_card_quantity")).isFalse();

        long deckId = insertDeck(connection, "Draft Deck", formatId, userId);
        long legalCard = insertCard(connection, "Legal Card", false, null);
        long bannedCard = insertCard(connection, "Banned Card", false, null);
        long basicLand = insertCard(connection, "Plains", true, null);

        insertLegality(connection, legalCard, formatId, "legal");
        insertLegality(connection, bannedCard, formatId, "banned");

        // Shape is still enforced: quantity and section.
        assertThatThrownBy(() -> insertDeckCard(connection, deckId, legalCard, 0, "main"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertDeckCard(connection, deckId, legalCard, 1, "banana"))
                .isInstanceOf(SQLException.class);

        // A draft may be illegal and incomplete: the database accepts it.
        insertDeckCard(connection, deckId, legalCard, 5, "main");
        insertDeckCard(connection, deckId, bannedCard, 3, "main");
        insertDeckCard(connection, deckId, basicLand, 40, "main");

        // One row per (deck, card, section) is still guaranteed.
        assertThatThrownBy(() -> insertDeckCard(connection, deckId, legalCard, 1, "main"))
                .isInstanceOf(SQLException.class);

        // copies_limit expresses NULL (defer), 0 (no limit) and N (exact cap).
        long unlimitedCard = insertCard(connection, "Persistent Petitioners", false, 0);
        long cappedCard = insertCard(connection, "Seven Dwarves", false, 7);
        assertThat(readInt(connection, "SELECT copies_limit FROM cards WHERE id = " + unlimitedCard)).isZero();
        assertThat(readInt(connection, "SELECT copies_limit FROM cards WHERE id = " + cappedCard)).isEqualTo(7);
        assertThat(readString(connection, "SELECT copies_limit::text FROM cards WHERE id = " + legalCard)).isNull();

        assertThatThrownBy(() -> execute(connection,
                "UPDATE cards SET copies_limit = -1 WHERE id = " + cappedCard))
                .isInstanceOf(SQLException.class);
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

    private static long insertCard(Connection connection, String name, boolean basicLand, Integer copiesLimit)
            throws Exception {
        String sql = """
                INSERT INTO cards(card_name, cmc, type_line, card_type, rarity, image_url, is_basic_land, copies_limit)
                VALUES (?, 1, 'Creature - Test', 'Creature', 'common', 'https://img', ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setBoolean(2, basicLand);
            statement.setObject(3, copiesLimit);
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

    private static void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
                SELECT min_deck_size, max_deck_size, max_sideboard_size, max_copies, singleton, requires_commander,
                       tracks_legality
                FROM formats
                WHERE format_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, formatName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("format %s exists", formatName).isTrue();
                return new FormatRules(
                        resultSet.getInt("min_deck_size"),
                        resultSet.getObject("max_deck_size", Integer.class),
                        resultSet.getObject("max_sideboard_size", Integer.class),
                        resultSet.getInt("max_copies"),
                        resultSet.getBoolean("singleton"),
                        resultSet.getBoolean("requires_commander"),
                        resultSet.getBoolean("tracks_legality"));
            }
        }
    }

    private record FormatRules(int minDeckSize, Integer maxDeckSize, Integer maxSideboardSize,
                               int maxCopies, boolean singleton, boolean requiresCommander,
                               boolean tracksLegality) {
    }

    private static String readString(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private static Integer readInt(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getObject(1, Integer.class);
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

    private static boolean hasFunction(Connection connection, String function) throws Exception {
        String sql = """
                SELECT 1
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'public'
                  AND p.proname = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, function);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
