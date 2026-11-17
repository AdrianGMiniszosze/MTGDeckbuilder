package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.importing.ImportCard;
import com.deckbuilder.mtgdeckbuilder.application.importing.ScryfallCardMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.CardImportDao;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.ScryfallBulkReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Card import (reader, mapper and card DAO)")
class CardImportIntegrationTest {

	private static final String FIXTURE = "importer/default-cards-sample.jsonl";
	private static final String FIXTURE_V2 = "importer/default-cards-sample-v2.jsonl";

	private static final UUID KRAV_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
	private static final UUID KRAV_ORACLE = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
	private static final UUID PETITIONERS_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
	private static final UUID DWARVES_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
	private static final UUID MOUNTAIN_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
	private static final UUID DELVER_ID = UUID.fromString("66666666-6666-4666-8666-666666666666");

	private static PostgreSQLContainer<?> postgres;
	private static JdbcTemplate jdbc;
	private static ScryfallBulkReader reader;
	private static CardImportDao dao;

	@BeforeAll
	static void startDatabase() {
		postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
			.withDatabaseName("mtg_import_test")
			.withUsername("test")
			.withPassword("test");
		postgres.start();

		Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.locations("classpath:db/migration")
			.load()
			.migrate();

		jdbc = new JdbcTemplate(new DriverManagerDataSource(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
		reader = new ScryfallBulkReader(new ObjectMapper());
	}

	@BeforeEach
	void resetDatabase() {
		jdbc.execute("TRUNCATE users, cards, sets RESTART IDENTITY CASCADE");
		// Fresh DAO so the set/format caches do not survive a truncate.
		dao = new CardImportDao(jdbc, new ObjectMapper());
	}

	@AfterAll
	static void stopDatabase() {
		if (postgres != null) {
			postgres.stop();
		}
	}

	@Test
	@DisplayName("Should import card rows and children twice without duplicating them")
	void shouldImportCardsIdempotently() throws Exception {
		final List<ImportCard> cards = readFixture(FIXTURE);
		assertThat(cards).hasSize(6);

		assertThat(upsertAll(cards)).isEqualTo(6);
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
		final long kravId = id(KRAV_ID);

		final Map<String, Object> krav = row(KRAV_ID);
		assertThat(krav.get("card_name")).isEqualTo("Krav, the Unredeemed");
		assertThat(krav.get("oracle_id")).isEqualTo(KRAV_ORACLE);
		assertThat(krav.get("mana_cost")).isEqualTo("{2}{B}{R}");
		assertThat(krav.get("cmc")).isEqualTo(4);
		assertThat(krav.get("color_identity")).isEqualTo("B,R");
		assertThat(krav.get("type_line")).isEqualTo("Legendary Creature — Demon");
		assertThat(krav.get("card_type")).isEqualTo("Creature");
		assertThat(krav.get("card_supertype")).isEqualTo("Legendary");
		assertThat(krav.get("rarity")).isEqualTo("rare");
		assertThat(krav.get("card_text")).isEqualTo("Flying.");
		assertThat(krav.get("flavor_text")).isNull();
		assertThat(krav.get("power")).isNull();
		assertThat(krav.get("toughness")).isNull();
		assertThat(krav.get("copies_limit")).isNull();
		assertThat(krav.get("is_basic_land")).isEqualTo(false);
		assertThat(krav.get("image_url")).isEqualTo("https://img.example/krav.jpg");
		assertThat(krav.get("foil")).isEqualTo(true);
		assertThat(krav.get("game_changer")).isEqualTo(true);
		assertThat(krav.get("language")).isEqualTo("en");
		assertThat(krav.get("collector_number")).isEqualTo("42");
		assertThat(krav.get("promo")).isEqualTo(true);
		assertThat(krav.get("variation")).isEqualTo(true);

		assertSet(krav);
		assertPrices(krav, new BigDecimal("1.25"), new BigDecimal("2.50"), null);

		assertThat(value(PETITIONERS_ID, "copies_limit")).isEqualTo(0);
		assertThat(value(DWARVES_ID, "copies_limit")).isEqualTo(7);
		assertThat(value(MOUNTAIN_ID, "is_basic_land")).isEqualTo(true);
		assertThat(value(MOUNTAIN_ID, "mana_cost")).isNull();
		assertThat(value(MOUNTAIN_ID, "color_identity")).isNull();
		assertThat(value(MOUNTAIN_ID, "usd")).isNull();
		assertThat(value(MOUNTAIN_ID, "price_observed_at")).isNull();
		assertThat(value(MOUNTAIN_ID, "set_id")).isNull();

		assertThat(strings(KRAV_ID, "card_colors", "color")).containsExactly("B", "R");
		assertThat(strings(KRAV_ID, "card_color_identity", "color")).containsExactly("B", "R");
		assertThat(strings(KRAV_ID, "card_types", "type")).containsExactly("Creature");
		assertThat(strings(KRAV_ID, "card_supertypes", "supertype")).containsExactly("Legendary");
		assertThat(strings(KRAV_ID, "card_subtypes", "subtype")).containsExactly("Demon");
		assertThat(strings(KRAV_ID, "card_keywords", "keyword")).containsExactly("Flying");
		assertThat(strings(DELVER_ID, "card_types", "type")).containsExactly("Creature");
		assertThat(strings(DELVER_ID, "card_subtypes", "subtype"))
			.containsExactly("Human", "Insect", "Wizard");
		assertThat(strings(MOUNTAIN_ID, "card_colors", "color")).isEmpty();
		assertThat(strings(MOUNTAIN_ID, "card_color_identity", "color")).isEmpty();

		assertThat(legality(KRAV_ID, "Standard")).isEqualTo("legal");
		assertThat(legality(KRAV_ID, "Modern")).isEqualTo("not_legal");
		assertThat(legality(KRAV_ID, "Commander")).isEqualTo("legal");
		assertThat(legality(KRAV_ID, "Vintage")).isEqualTo("restricted");
		assertThat(legality(KRAV_ID, "Legacy")).isNull();
		assertThat(legality(KRAV_ID, "Alchemy")).isNull();
		assertThat(count("SELECT COUNT(*) FROM card_legality WHERE card_id = ?", kravId)).isEqualTo(4);
		assertThat(count("SELECT COUNT(*) FROM formats")).isEqualTo(7);

		assertThat(value(DELVER_ID, "card_text"))
			.isEqualTo("At the beginning of your upkeep, look at the top card of your library."
					+ "\n\nFlying.");
		assertThat(count("SELECT COUNT(*) FROM card_faces WHERE card_id = ?", id(DELVER_ID)))
			.isEqualTo(2);
		assertThat(faceValue(DELVER_ID, 0, "name")).isEqualTo("Delver of Secrets");
		assertThat(faceValue(DELVER_ID, 0, "mana_cost")).isEqualTo("{U}");
		assertThat(faceValue(DELVER_ID, 0, "power")).isEqualTo("1");
		assertThat(faceValue(DELVER_ID, 0, "face_data->>'name'")).isEqualTo("Delver of Secrets");
		assertThat(faceValue(DELVER_ID, 0, "colors->>0")).isEqualTo("U");
		assertThat(faceValue(DELVER_ID, 0, "image_uris->>'normal'"))
			.isEqualTo("https://img.example/delver.jpg");
		assertThat(faceValue(DELVER_ID, 1, "name")).isEqualTo("Insectile Aberration");
		assertThat(faceValue(DELVER_ID, 1, "mana_cost")).isEmpty();
		assertThat(faceValue(DELVER_ID, 1, "image_uris")).isNull();

		assertThat(count("SELECT COUNT(*) FROM cards WHERE card_name IN ('Soldier', 'Emblem', 'Art Series')"))
			.isZero();
		assertThat(count("SELECT COUNT(*) FROM cards WHERE oracle_id = ?", KRAV_ORACLE)).isEqualTo(2);

		// A second pass refreshes the same rows and reports updates, not inserts.
		assertThat(upsertAll(readFixture(FIXTURE))).isZero();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
		assertThat(id(KRAV_ID)).isEqualTo(kravId);
		assertThat(count("SELECT COUNT(*) FROM card_colors")).isEqualTo(7);
		assertThat(count("SELECT COUNT(*) FROM card_keywords")).isEqualTo(3);
		assertThat(count("SELECT COUNT(*) FROM card_legality")).isEqualTo(10);
		assertThat(count("SELECT COUNT(*) FROM card_faces")).isEqualTo(2);
		assertThat(count("SELECT COUNT(*) FROM sets")).isEqualTo(1);
	}

	@Test
	@DisplayName("Should replace children, legalities, faces and prices on reimport")
	void shouldReplaceChildrenOnReimport() throws Exception {
		upsertAll(readFixture(FIXTURE));
		final long kravId = id(KRAV_ID);

		final long userId = insertUser();
		final long deckId = insertDeck(userId, kravId);

		assertThat(upsertAll(readFixture(FIXTURE_V2))).isZero();

		assertThat(id(KRAV_ID)).isEqualTo(kravId);
		assertThat(strings(KRAV_ID, "card_keywords", "keyword")).isEmpty();
		assertThat(count("SELECT COUNT(*) FROM card_keywords WHERE card_id = ?", kravId)).isZero();
		assertThat(legality(KRAV_ID, "Standard")).isEqualTo("banned");
		assertThat(legality(KRAV_ID, "Legacy")).isNull();
		assertThat((BigDecimal) value(KRAV_ID, "usd")).isEqualByComparingTo("9.99");

		assertThat(value(KRAV_ID, "usd_foil")).isNull();
		assertThat(value(KRAV_ID, "price_observed_at")).isNotNull();

		assertThat(count("SELECT COUNT(*) FROM card_faces WHERE card_id = ?", id(DELVER_ID)))
			.isEqualTo(2);
		assertThat(faceValue(DELVER_ID, 0, "oracle_text"))
			.isEqualTo("At the beginning of your upkeep, look at the top two cards of your library.");
		assertThat(faceValue(DELVER_ID, 1, "oracle_text")).isEqualTo("Flying. Ward {2}.");

		// Reimporting never touches deck contents.
		assertThat(count("SELECT COUNT(*) FROM card_deck WHERE deck_id = ? AND card_id = ?", deckId, kravId))
			.isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM decks WHERE id = ?", deckId)).isEqualTo(1);
	}

	@Test
	@DisplayName("Should fill a set code when the row already exists")
	void shouldFillMissingSetCode() throws Exception {
		jdbc.update("INSERT INTO sets (name) VALUES (?)", "Test Set");

		upsertAll(readFixture(FIXTURE));

		final Map<String, Object> set = jdbc.queryForMap(
				"SELECT * FROM sets WHERE name = ?", "Test Set");
		assertThat(set.get("code")).isEqualTo("tst");
		assertThat(localDate(set.get("released_at"))).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(set.get("set_type")).isEqualTo("expansion");
		assertThat(count("SELECT COUNT(*) FROM sets")).isEqualTo(1);
	}

	private static void assertSet(Map<String, Object> card) {
		assertThat(card.get("set_id")).isNotNull();
		final Map<String, Object> set = jdbc.queryForMap(
				"SELECT * FROM sets WHERE id = ?", card.get("set_id"));
		assertThat(set.get("name")).isEqualTo("Test Set");
		assertThat(set.get("code")).isEqualTo("tst");
		assertThat(localDate(set.get("released_at"))).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(set.get("set_type")).isEqualTo("expansion");
	}

	private static LocalDate localDate(Object value) {
		return value == null ? null : ((java.sql.Date) value).toLocalDate();
	}

	private static void assertPrices(Map<String, Object> card, BigDecimal usd, BigDecimal usdFoil,
			BigDecimal eur) {
		assertDecimal(card.get("usd"), usd);
		assertDecimal(card.get("usd_foil"), usdFoil);
		assertDecimal(card.get("eur"), eur);
		assertThat(card.get("price_observed_at")).isNotNull();
	}

	private static void assertDecimal(Object actual, BigDecimal expected) {
		if (expected == null) {
			assertThat(actual).isNull();
		} else {
			assertThat((BigDecimal) actual).isEqualByComparingTo(expected);
		}
	}

	private static long insertUser() {
		return jdbc.queryForObject("""
				INSERT INTO users (name, username, email, hashed_password)
				VALUES ('Importer', 'importer', 'importer@example.com', 'hash')
				RETURNING id
				""", Long.class);
	}

	private static long insertDeck(long userId, long cardId) {
		final Long formatId = jdbc.queryForObject(
				"SELECT id FROM formats WHERE format_name = 'Standard'", Long.class);
		final long deckId = jdbc.queryForObject("""
				INSERT INTO decks (deck_name, format_id, user_id)
				VALUES ('Importer deck', ?, ?)
				RETURNING id
				""", Long.class, formatId, userId);
		jdbc.update("INSERT INTO card_deck (deck_id, card_id, quantity) VALUES (?, ?, 1)",
				deckId, cardId);
		return deckId;
	}

	private static List<ImportCard> readFixture(String resource) throws Exception {
		final Path fixture = Path.of(Objects
			.requireNonNull(CardImportIntegrationTest.class.getClassLoader().getResource(resource))
			.toURI());
		try (Stream<JsonNode> stream = reader.read(fixture)) {
			return stream.map(ScryfallCardMapper::toImportCard)
				.flatMap(Optional::stream)
				.toList();
		}
	}

	private static long upsertAll(List<ImportCard> cards) {
		return cards.stream().filter(card -> dao.upsert(card).inserted()).count();
	}

	private static int count(String sql, Object... args) {
		return jdbc.queryForObject(sql, Integer.class, args);
	}

	private static long id(UUID scryfallId) {
		return jdbc.queryForObject(
				"SELECT id FROM cards WHERE scryfall_id = ?", Long.class, scryfallId);
	}

	private static Map<String, Object> row(UUID scryfallId) {
		return jdbc.queryForMap("SELECT * FROM cards WHERE scryfall_id = ?", scryfallId);
	}

	private static Object value(UUID scryfallId, String column) {
		return row(scryfallId).get(column);
	}

	private static List<String> strings(UUID scryfallId, String table, String column) {
		// Table and column are compile-time constants in this test.
		return jdbc.queryForList(
				"SELECT " + column + " FROM " + table + " WHERE card_id = ? ORDER BY " + column,
				String.class, id(scryfallId));
	}

	private static String legality(UUID scryfallId, String formatName) {
		final List<String> statuses = jdbc.queryForList("""
				SELECT cl.legality_status
				FROM card_legality cl
				JOIN formats f ON f.id = cl.format_id
				WHERE cl.card_id = ? AND f.format_name = ?
				""", String.class, id(scryfallId), formatName);
		return statuses.isEmpty() ? null : statuses.get(0);
	}

	private static String faceValue(UUID scryfallId, int faceIndex, String column) {
		return jdbc.queryForObject(
				"SELECT " + column + " FROM card_faces WHERE card_id = ? AND face_index = ?",
				String.class, id(scryfallId), faceIndex);
	}
}
