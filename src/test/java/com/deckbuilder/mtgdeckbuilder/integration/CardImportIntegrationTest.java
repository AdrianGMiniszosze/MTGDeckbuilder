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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Card import (reader, mapper and card DAO)")
class CardImportIntegrationTest {

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
		dao = new CardImportDao(jdbc);
	}

	@AfterAll
	static void stopDatabase() {
		if (postgres != null) {
			postgres.stop();
		}
	}

	@Test
	@DisplayName("Should import playable printings twice without duplicating rows")
	void shouldImportCardRowsIdempotently() throws Exception {
		final List<ImportCard> cards = readFixture();
		assertThat(cards).hasSize(6);

		assertThat(upsertAll(cards)).isEqualTo(6);
		assertThat(countCards()).isEqualTo(6);

		final Map<String, Object> krav = jdbc.queryForMap(
				"SELECT * FROM cards WHERE scryfall_id = ?", KRAV_ID);
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

		assertThat(value(PETITIONERS_ID, "copies_limit")).isEqualTo(0);
		assertThat(value(DWARVES_ID, "copies_limit")).isEqualTo(7);
		assertThat(value(MOUNTAIN_ID, "is_basic_land")).isEqualTo(true);
		assertThat(value(MOUNTAIN_ID, "mana_cost")).isNull();
		assertThat(value(MOUNTAIN_ID, "color_identity")).isNull();
		assertThat(value(DELVER_ID, "image_url")).isEqualTo("https://img.example/delver.jpg");

		assertThat(countCards()).isEqualTo(6);
		assertThat(countSkipped()).isZero();
		assertThat(countByOracle(KRAV_ORACLE)).isEqualTo(2);

		// A second pass refreshes the same rows and reports updates, not inserts.
		assertThat(upsertAll(readFixture())).isZero();
		assertThat(countCards()).isEqualTo(6);
	}

	private static List<ImportCard> readFixture() throws Exception {
		final Path fixture = Path.of(Objects
			.requireNonNull(CardImportIntegrationTest.class.getClassLoader()
				.getResource("importer/default-cards-sample.jsonl"))
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

	private static int countCards() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM cards", Integer.class);
	}

	private static int countSkipped() {
		return jdbc.queryForObject(
				"SELECT COUNT(*) FROM cards WHERE card_name IN ('Soldier', 'Emblem', 'Art Series')",
				Integer.class);
	}

	private static int countByOracle(UUID oracleId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM cards WHERE oracle_id = ?", Integer.class, oracleId);
	}

	private static Object value(UUID scryfallId, String column) {
		return jdbc.queryForMap("SELECT " + column + " FROM cards WHERE scryfall_id = ?", scryfallId).get(column);
	}
}
