package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.importing.CardImportService;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportCard;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportRuling;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportSummary;
import com.deckbuilder.mtgdeckbuilder.importer.ImportOptions;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.CardImportDao;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.ImportLock;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.RulingImportDao;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.ScryfallBulkReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Card import service (end to end on PostgreSQL)")
class CardImportServiceIntegrationTest {

	private static final String CARDS = "importer/default-cards-sample.jsonl";
	private static final String CARDS_V2 = "importer/default-cards-sample-v2.jsonl";
	private static final String RULINGS = "importer/rulings-sample.jsonl";
	private static final String RULINGS_V2 = "importer/rulings-sample-v2.jsonl";

	private static final UUID KRAV_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

	@TempDir
	Path tempDir;

	private static PostgreSQLContainer<?> postgres;
	private static DataSource dataSource;
	private static JdbcTemplate jdbc;
	private static ScryfallBulkReader reader;

	private CardImportDao cardDao;
	private RulingImportDao rulingDao;

	@BeforeAll
	static void startDatabase() {
		postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
			.withDatabaseName("mtg_service_test")
			.withUsername("test")
			.withPassword("test");
		postgres.start();

		Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.locations("classpath:db/migration")
			.load()
			.migrate();

		dataSource = new DriverManagerDataSource(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		jdbc = new JdbcTemplate(dataSource);
		reader = new ScryfallBulkReader(new ObjectMapper());
	}

	@BeforeEach
	void resetDatabase() {
		jdbc.execute("""
				TRUNCATE users, cards, sets, card_rulings, import_seen, import_ruling
				RESTART IDENTITY CASCADE
				""");
		// Fresh DAOs so the set/format caches do not survive a truncate.
		cardDao = new CardImportDao(jdbc, new ObjectMapper());
		rulingDao = new RulingImportDao(jdbc);
	}

	@AfterAll
	static void stopDatabase() {
		if (postgres != null) {
			postgres.stop();
		}
	}

	@Test
	@DisplayName("Should import cards and rulings and clean its staging tables")
	void shouldImportCardsAndRulings() throws Exception {
		final ImportOptions options = options(CARDS, RULINGS);
		options.setBatchSize(1);

		final ImportSummary summary = service().importAll(options);

		assertThat(summary.cardsInserted()).isEqualTo(6);
		assertThat(summary.cardsUpdated()).isZero();
		// The fixture also carries a token, an emblem and an art series.
		assertThat(summary.cardsSkipped()).isEqualTo(3);
		assertThat(summary.cardsFailed()).isZero();
		assertThat(summary.cardsObsolete()).isZero();
		assertThat(summary.rulingsStaged()).isEqualTo(3);
		assertThat(summary.rulingsInserted()).isEqualTo(3);
		assertThat(summary.rulingsDeleted()).isZero();
		assertThat(summary.rulingsSkipped()).isZero();
		assertThat(summary.rulingsFailed()).isZero();
		assertThat(summary.partial()).isFalse();
		assertThat(summary.sweepSkipped()).isFalse();
		assertThat(summary.rulingMirrorSkipped()).isFalse();
		assertThat(summary.hasFailures()).isFalse();
		assertThat(summary.duration()).isNotNull();
		assertThat(summary.toString()).contains("cards inserted=6");

		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
		assertThat(count("SELECT COUNT(*) FROM cards WHERE is_obsolete")).isZero();
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(3);
		assertThat(count("SELECT COUNT(*) FROM import_seen")).isZero();
		assertThat(count("SELECT COUNT(*) FROM import_ruling")).isZero();
	}

	@Test
	@DisplayName("Should refresh rows on a second run without duplicating them")
	void shouldUpdateOnSecondRun() throws Exception {
		final ImportOptions options = options(CARDS, RULINGS);
		options.setBatchSize(1);
		service().importAll(options);

		final ImportSummary summary = service().importAll(options);

		assertThat(summary.cardsInserted()).isZero();
		assertThat(summary.cardsUpdated()).isEqualTo(6);
		assertThat(summary.cardsFailed()).isZero();
		assertThat(summary.cardsObsolete()).isZero();
		assertThat(summary.rulingsStaged()).isEqualTo(3);
		assertThat(summary.rulingsInserted()).isZero();
		assertThat(summary.rulingsDeleted()).isZero();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(3);
	}

	@Test
	@DisplayName("Should mark vanished cards obsolete and mirror corrected rulings")
	void shouldSweepAndMirrorCorrections() throws Exception {
		service().importAll(options(CARDS, RULINGS));

		final ImportSummary summary = service().importAll(options(CARDS_V2, RULINGS_V2));

		assertThat(summary.cardsInserted()).isZero();
		assertThat(summary.cardsUpdated()).isEqualTo(2);
		assertThat(summary.cardsObsolete()).isEqualTo(4);
		assertThat(summary.sweepSkipped()).isFalse();
		assertThat(summary.rulingsStaged()).isEqualTo(3);
		assertThat(summary.rulingsDeleted()).isEqualTo(2);
		assertThat(summary.rulingsInserted()).isEqualTo(2);
		assertThat(summary.rulingMirrorSkipped()).isFalse();
		assertThat(count("SELECT COUNT(*) FROM cards WHERE is_obsolete")).isEqualTo(4);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(3);
		assertThat(rulingComments()).containsExactlyInAnyOrder(
				"Krav ruling one, corrected.",
				"Delver ruling.",
				"Delver second ruling.");
	}

	@Test
	@DisplayName("Should skip the sweep and the ruling mirror during a partial load")
	void shouldSkipDestructiveStepsInPartialLoad() throws Exception {
		final ImportOptions options = options(CARDS, RULINGS);
		options.setLimit(2);

		final ImportSummary summary = service().importAll(options);

		assertThat(summary.cardsInserted()).isEqualTo(2);
		assertThat(summary.cardsObsolete()).isZero();
		assertThat(summary.partial()).isTrue();
		assertThat(summary.sweepSkipped()).isTrue();
		assertThat(summary.rulingsStaged()).isZero();
		assertThat(summary.rulingsInserted()).isZero();
		assertThat(summary.rulingMirrorSkipped()).isTrue();
		assertThat(summary.hasFailures()).isFalse();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(2);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isZero();
	}

	@Test
	@DisplayName("Should insert the good rows when one row in a batch fails")
	void shouldInsertGoodRowsWhenOneFails() throws Exception {
		final CardImportDao failing = new FailingCardImportDao(jdbc, KRAV_ID);

		final ImportSummary summary = service(failing, rulingDao).importAll(options(CARDS, null));

		assertThat(summary.cardsInserted()).isEqualTo(5);
		assertThat(summary.cardsFailed()).isEqualTo(1);
		assertThat(summary.sweepSkipped()).isTrue();
		assertThat(summary.hasFailures()).isTrue();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(5);
	}

	@Test
	@DisplayName("Should count a failed card row and skip the sweep")
	void shouldSkipSweepWhenCardRowsFail() throws Exception {
		service().importAll(options(CARDS, null));

		final CardImportDao failing = new FailingCardImportDao(jdbc, KRAV_ID);
		final ImportSummary summary = service(failing, rulingDao).importAll(options(CARDS_V2, null));

		assertThat(summary.cardsUpdated()).isEqualTo(1);
		assertThat(summary.cardsFailed()).isEqualTo(1);
		assertThat(summary.cardsObsolete()).isZero();
		assertThat(summary.sweepSkipped()).isTrue();
		assertThat(summary.hasFailures()).isTrue();
		assertThat(count("SELECT COUNT(*) FROM cards WHERE is_obsolete")).isZero();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
	}

	@Test
	@DisplayName("Should skip the ruling mirror when a ruling row fails")
	void shouldSkipMirrorWhenRulingRowsFail() throws Exception {
		jdbc.update("INSERT INTO card_rulings (oracle_id, published_at, comment, source) "
				+ "VALUES (?, DATE '2025-01-01', 'Kept', 'wotc')", UUID.randomUUID());

		final RulingImportDao failing = new FailingRulingImportDao(jdbc, "Krav ruling two.");
		final ImportSummary summary = service(cardDao, failing).importAll(options(CARDS, RULINGS));

		assertThat(summary.cardsInserted()).isEqualTo(6);
		assertThat(summary.rulingsStaged()).isEqualTo(2);
		assertThat(summary.rulingsInserted()).isZero();
		assertThat(summary.rulingsDeleted()).isZero();
		assertThat(summary.rulingsFailed()).isEqualTo(1);
		assertThat(summary.rulingMirrorSkipped()).isTrue();
		assertThat(summary.hasFailures()).isTrue();
		assertThat(rulingComments()).containsExactly("Kept");
	}

	@Test
	@DisplayName("Should count malformed lines instead of aborting the run")
	void shouldCountMalformedLines() throws Exception {
		final Path cards = write("cards.jsonl",
				Files.readAllLines(fixturePath(CARDS)).get(0),
				"{not json");
		final Path rulings = write("rulings.jsonl",
				Files.readAllLines(fixturePath(RULINGS)).get(0),
				"{\"oracle_id\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\"}",
				"{not json");
		final ImportOptions options = new ImportOptions();
		options.setFile(cards);
		options.setRulings(rulings);

		final ImportSummary summary = service().importAll(options);

		assertThat(summary.cardsInserted()).isEqualTo(1);
		assertThat(summary.cardsFailed()).isEqualTo(1);
		assertThat(summary.sweepSkipped()).isTrue();
		assertThat(summary.rulingsStaged()).isEqualTo(1);
		assertThat(summary.rulingsSkipped()).isEqualTo(1);
		assertThat(summary.rulingsFailed()).isEqualTo(1);
		assertThat(summary.rulingMirrorSkipped()).isTrue();
		assertThat(summary.hasFailures()).isTrue();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isZero();
	}

	@Test
	@DisplayName("Should abort when the cards file produces nothing")
	void shouldAbortOnEmptyCardsFile() throws Exception {
		final ImportOptions options = new ImportOptions();
		options.setFile(write("cards.jsonl", "   ", ""));

		assertThatThrownBy(() -> service().importAll(options))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("no cards");

		assertThat(count("SELECT COUNT(*) FROM cards")).isZero();
	}

	@Test
	@DisplayName("Should abort when the rulings file produces nothing")
	void shouldAbortOnEmptyRulingsFile() throws Exception {
		final ImportOptions options = new ImportOptions();
		options.setFile(fixturePath(CARDS));
		options.setRulings(write("rulings.jsonl", ""));

		assertThatThrownBy(() -> service().importAll(options))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("no rows");

		// Cards are committed before rulings; the mirror is the only refusal.
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isZero();
		assertThat(count("SELECT COUNT(*) FROM import_seen")).isZero();
	}

	@Test
	@DisplayName("Should log cleanup failures without failing a finished import")
	void shouldTolerateCleanupFailures() throws Exception {
		final CardImportDao failingCards = new FailingCleanupCardImportDao(jdbc);
		final RulingImportDao failingRulings = new FailingCleanupRulingImportDao(jdbc);

		final ImportSummary summary = service(failingCards, failingRulings)
			.importAll(options(CARDS, RULINGS));

		assertThat(summary.hasFailures()).isFalse();
		assertThat(count("SELECT COUNT(*) FROM cards")).isEqualTo(6);
		assertThat(count("SELECT COUNT(*) FROM card_rulings")).isEqualTo(3);
		// Cleanup failed on purpose, so each staging table keeps its rows.
		assertThat(count("SELECT COUNT(*) FROM import_seen")).isEqualTo(6);
		assertThat(count("SELECT COUNT(*) FROM import_ruling")).isEqualTo(3);
	}

	@Test
	@DisplayName("Should refuse to run while another import holds the lock")
	void shouldRefuseToRunWhileLocked() throws Exception {
		final ImportOptions options = options(CARDS, null);

		try (ImportLock lock = ImportLock.tryAcquire(dataSource).orElseThrow()) {
			assertThat(lock).isNotNull();
			assertThatThrownBy(() -> service().importAll(options))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already running");
		}

		assertThat(count("SELECT COUNT(*) FROM cards")).isZero();
	}

	private CardImportService service() {
		return service(cardDao, rulingDao);
	}

	private static CardImportService service(CardImportDao cards, RulingImportDao rulings) {
		return new CardImportService(cards, rulings, reader, dataSource,
				new DataSourceTransactionManager(dataSource));
	}

	private ImportOptions options(String cards, String rulings) {
		final ImportOptions options = new ImportOptions();
		options.setFile(fixturePath(cards));
		options.setRulings(rulings == null ? null : fixturePath(rulings));
		return options;
	}

	private Path write(String name, String... lines) throws Exception {
		final Path file = tempDir.resolve(name);
		Files.write(file, List.of(lines));
		return file;
	}

	private static Path fixturePath(String resource) {
		try {
			return Path.of(Objects
				.requireNonNull(CardImportServiceIntegrationTest.class.getClassLoader()
					.getResource(resource))
				.toURI());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static List<String> rulingComments() {
		return jdbc.queryForList("SELECT comment FROM card_rulings", String.class);
	}

	private static int count(String sql, Object... args) {
		return jdbc.queryForObject(sql, Integer.class, args);
	}

	/** Fails only for one printing, so the batch fails and the row retry isolates it. */
	private static final class FailingCardImportDao extends CardImportDao {

		private final UUID failingId;

		FailingCardImportDao(JdbcTemplate jdbc, UUID failingId) {
			super(jdbc, new ObjectMapper());
			this.failingId = failingId;
		}

		@Override
		public UpsertResult upsert(ImportCard card, UUID runId) {
			if (failingId.equals(card.scryfallId())) {
				throw new IllegalStateException("card write failed on purpose");
			}
			return super.upsert(card, runId);
		}
	}

	/** Fails only for one comment, so a batch fails and the row retry isolates it. */
	private static final class FailingRulingImportDao extends RulingImportDao {
		private final String failingComment;

		FailingRulingImportDao(JdbcTemplate jdbc, String failingComment) {
			super(jdbc);
			this.failingComment = failingComment;
		}

		@Override
		public void stage(List<ImportRuling> rulings, UUID runId) {
			for (final ImportRuling ruling : rulings) {
				stage(ruling, runId);
			}
		}

		@Override
		public void stage(ImportRuling ruling, UUID runId) {
			if (failingComment.equals(ruling.comment())) {
				throw new IllegalStateException("ruling write failed on purpose");
			}
			super.stage(ruling, runId);
		}
	}

	/** Leaves its own staging rows behind to exercise the cleanup warning path. */
	private static final class FailingCleanupCardImportDao extends CardImportDao {

		FailingCleanupCardImportDao(JdbcTemplate jdbc) {
			super(jdbc, new ObjectMapper());
		}

		@Override
		public int clearSeen(UUID runId) {
			throw new IllegalStateException("cleanup failed on purpose");
		}
	}

	/** Leaves its own staging rows behind to exercise the cleanup warning path. */
	private static final class FailingCleanupRulingImportDao extends RulingImportDao {

		FailingCleanupRulingImportDao(JdbcTemplate jdbc) {
			super(jdbc);
		}

		@Override
		public int clearStaged(UUID runId) {
			throw new IllegalStateException("cleanup failed on purpose");
		}
	}
}
