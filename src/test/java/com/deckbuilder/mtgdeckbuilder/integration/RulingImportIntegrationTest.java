package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.importing.ImportRuling;
import com.deckbuilder.mtgdeckbuilder.application.importing.ScryfallRulingMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.RulingImportDao;
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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ruling import staging and mirror")
class RulingImportIntegrationTest {

	private static final String FIXTURE = "importer/rulings-sample.jsonl";
	private static final String FIXTURE_V2 = "importer/rulings-sample-v2.jsonl";

	private static PostgreSQLContainer<?> postgres;
	private static JdbcTemplate jdbc;
	private static ScryfallBulkReader reader;
	private static RulingImportDao dao;

	@BeforeAll
	static void startDatabase() {
		postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
			.withDatabaseName("mtg_ruling_test")
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
		dao = new RulingImportDao(jdbc);
	}

	@BeforeEach
	void resetDatabase() {
		jdbc.execute("TRUNCATE card_rulings, import_ruling");
	}

	@AfterAll
	static void stopDatabase() {
		if (postgres != null) {
			postgres.stop();
		}
	}

	@Test
	@DisplayName("Should mirror corrections and removals without a ruling id")
	void shouldMirrorCorrectionsAndRemovals() throws Exception {
		final UUID first = UUID.randomUUID();
		dao.stage(readFixture(FIXTURE), first);
		assertThat(dao.countStaged(first)).isEqualTo(3);

		final RulingImportDao.RulingApplyResult initial = dao.apply(first);
		assertThat(initial.deleted()).isZero();
		assertThat(initial.inserted()).isEqualTo(3);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(3);

		final UUID second = UUID.randomUUID();
		dao.stage(readFixture(FIXTURE_V2), second);
		final RulingImportDao.RulingApplyResult mirror = dao.apply(second);

		assertThat(mirror.deleted()).isEqualTo(2);
		assertThat(mirror.inserted()).isEqualTo(2);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(3);
		assertThat(comments()).containsExactlyInAnyOrder(
				"Krav ruling one, corrected.",
				"Delver ruling.",
				"Delver second ruling.");
		assertThat(comments()).doesNotContain("Krav ruling one.", "Krav ruling two.");
		assertThat(count("""
				SELECT COUNT(*) FROM card_rulings
				WHERE oracle_id = ? AND source = 'wotc' AND published_at = DATE '2026-02-01'
				""", UUID.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"))).isEqualTo(2);
	}

	@Test
	@DisplayName("Should refuse to mirror an empty staging run")
	void shouldRefuseEmptyMirror() {
		jdbc.update("INSERT INTO card_rulings (oracle_id, published_at, comment, source) "
				+ "VALUES (?, DATE '2026-01-01', 'Kept', 'wotc')", UUID.randomUUID());

		assertThatThrownBy(() -> dao.apply(UUID.randomUUID()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("no staged rows");

		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(1);
	}

	@Test
	@DisplayName("Should clear the current run and stale staging runs")
	void shouldClearStaging() throws Exception {
		final UUID previous = UUID.randomUUID();
		dao.stage(readFixture(FIXTURE), previous);
		assertThat(dao.countStaged(previous)).isEqualTo(3);

		final UUID current = UUID.randomUUID();
		assertThat(dao.clearOtherRuns(current)).isEqualTo(3);
		assertThat(dao.countStaged(previous)).isZero();

		dao.stage(readFixture(FIXTURE), current);
		assertThat(dao.clearStaged(current)).isEqualTo(3);
		assertThat(count("SELECT COUNT(*) FROM import_ruling")).isZero();
	}

	private static List<String> comments() {
		return jdbc.queryForList("SELECT comment FROM card_rulings", String.class);
	}

	private static int count(String sql, Object... args) {
		return jdbc.queryForObject(sql, Integer.class, args);
	}

	private static List<ImportRuling> readFixture(String resource) throws Exception {
		final Path fixture = Path.of(Objects
			.requireNonNull(RulingImportIntegrationTest.class.getClassLoader().getResource(resource))
			.toURI());
		try (Stream<JsonNode> stream = reader.read(fixture)) {
			return stream.map(ScryfallRulingMapper::toImportRuling)
				.flatMap(Optional::stream)
				.toList();
		}
	}
}
