package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.deckbuilder.mtgdeckbuilder.importer.ImportOptions;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.CardImportDao;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.ImportLock;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.RulingImportDao;
import com.deckbuilder.mtgdeckbuilder.infrastructure.importer.ScryfallBulkReader;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Orchestrates one Scryfall import: locking, streaming, batches, the obsolete
 * sweep and the ruling mirror.
 *
 * <p>
 * A failed card or ruling is counted and the run continues. If anything failed,
 * the destructive steps (card sweep, ruling mirror) are skipped: a row that
 * could not be read must not be interpreted as a row that disappeared from the
 * catalogue. Row failures therefore return exit code 1 through
 * {@link ImportSummary#hasFailures()}; configuration, IO and lock problems are
 * thrown and handled as exit code 2 by the runner.
 */
@Service
@Profile("import")
public class CardImportService {

	private static final Logger log = LoggerFactory.getLogger(CardImportService.class);

	private final CardImportDao cardImportDao;
	private final RulingImportDao rulingImportDao;
	private final ScryfallBulkReader reader;
	private final DataSource dataSource;
	private final TransactionTemplate transactionTemplate;

	public CardImportService(CardImportDao cardImportDao, RulingImportDao rulingImportDao,
			ScryfallBulkReader reader, DataSource dataSource,
			PlatformTransactionManager transactionManager) {
		this.cardImportDao = cardImportDao;
		this.rulingImportDao = rulingImportDao;
		this.reader = reader;
		this.dataSource = dataSource;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public ImportSummary importAll(ImportOptions options) throws IOException, SQLException {
		options.validate();
		final Instant started = Instant.now();
		final Optional<ImportLock> acquired = ImportLock.tryAcquire(dataSource);
		if (acquired.isEmpty()) {
			throw new IllegalStateException("Another import is already running");
		}

		try (ImportLock lock = acquired.get()) {
			final UUID runId = UUID.randomUUID();
			cardImportDao.clearOtherRuns(runId);
			rulingImportDao.clearOtherRuns(runId);
			try {
				final CardCounts cards = importCards(options, runId);
				final RulingCounts rulings = options.getRulings() == null
						? RulingCounts.none(false)
						: importRulings(options, runId);
				return new ImportSummary(
						cards.inserted(), cards.updated(), cards.skipped(), cards.failed(),
						cards.obsolete(), rulings.staged(), rulings.inserted(), rulings.deleted(),
						rulings.skipped(), rulings.failed(), options.getLimit() != null,
						cards.sweepSkipped(), rulings.mirrorSkipped(),
						Duration.between(started, Instant.now()));
			}
			finally {
				clearRun(runId);
			}
		}
	}

	private CardCounts importCards(ImportOptions options, UUID runId) throws IOException {
		int processed = 0;
		int skipped = 0;
		int failed = 0;
		int inserted = 0;
		int updated = 0;
		final List<ImportCard> batch = new ArrayList<>(options.getBatchSize());

		try (Stream<String> lines = reader.lines(options.getFile())) {
			final Iterator<String> iterator = lines.iterator();
			while (iterator.hasNext() && !limitReached(options, processed)) {
				final String line = iterator.next();
				processed++;
				final Optional<ImportCard> card;
				try {
					final JsonNode node = reader.parse(line);
					card = ScryfallCardMapper.toImportCard(node);
				}
				catch (RuntimeException ex) {
					failed++;
					log.warn("Card line {} skipped: {}", processed, ex.getMessage());
					continue;
				}
				if (card.isEmpty()) {
					skipped++;
					continue;
				}
				batch.add(card.get());
				if (batch.size() >= options.getBatchSize()) {
					final BatchTotals totals = writeCards(batch, runId);
					inserted += totals.inserted();
					updated += totals.updated();
					failed += totals.failed();
					batch.clear();
				}
			}
		}

		if (!batch.isEmpty()) {
			final BatchTotals totals = writeCards(batch, runId);
			inserted += totals.inserted();
			updated += totals.updated();
			failed += totals.failed();
		}

		final boolean partial = options.getLimit() != null;
		final boolean sweepSkipped = partial || failed > 0;
		int obsolete = 0;
		if (sweepSkipped) {
			log.warn("Obsolete sweep skipped (partial={}, failed rows={})", partial, failed);
		}
		else {
			if (cardImportDao.countSeen(runId) == 0) {
				throw new IllegalStateException(
						"Import produced no cards; refusing to mark the catalogue obsolete");
			}
			obsolete = cardImportDao.markObsolete(runId);
		}
		return new CardCounts(inserted, updated, skipped, failed, obsolete, sweepSkipped);
	}

	private BatchTotals writeCards(List<ImportCard> batch, UUID runId) {
		final int[] totals = new int[3];
		try {
			transactionTemplate.executeWithoutResult(status -> {
				for (final ImportCard card : batch) {
					final CardImportDao.UpsertResult result = cardImportDao.upsert(card, runId);
					totals[result.inserted() ? 0 : 1]++;
				}
			});
		}
		catch (RuntimeException batchFailure) {
			log.warn("Card batch of {} failed, retrying row by row: {}",
					batch.size(), batchFailure.getMessage());
			totals[0] = 0;
			totals[1] = 0;
			for (final ImportCard card : batch) {
				try {
					transactionTemplate.executeWithoutResult(status -> {
						final CardImportDao.UpsertResult result = cardImportDao.upsert(card, runId);
						totals[result.inserted() ? 0 : 1]++;
					});
				}
				catch (RuntimeException rowFailure) {
					totals[2]++;
					log.warn("Card {} failed: {}", card.scryfallId(), rowFailure.getMessage());
				}
			}
		}
		return new BatchTotals(totals[0], totals[1], totals[2]);
	}

	private RulingCounts importRulings(ImportOptions options, UUID runId) throws IOException {
		if (options.getLimit() != null) {
			log.warn("Rulings skipped during a partial load; the mirror needs the full file");
			return RulingCounts.none(true);
		}

		int processed = 0;
		int skipped = 0;
		int failed = 0;
		final List<ImportRuling> batch = new ArrayList<>(options.getBatchSize());

		try (Stream<String> lines = reader.lines(options.getRulings())) {
			final Iterator<String> iterator = lines.iterator();
			while (iterator.hasNext()) {
				final String line = iterator.next();
				processed++;
				final Optional<ImportRuling> ruling;
				try {
					final JsonNode node = reader.parse(line);
					ruling = ScryfallRulingMapper.toImportRuling(node);
				}
				catch (RuntimeException ex) {
					failed++;
					log.warn("Ruling line {} skipped: {}", processed, ex.getMessage());
					continue;
				}
				if (ruling.isEmpty()) {
					skipped++;
					continue;
				}
				batch.add(ruling.get());
				if (batch.size() >= options.getBatchSize()) {
					failed += stageRulings(batch, runId);
					batch.clear();
				}
			}
		}

		if (!batch.isEmpty()) {
			failed += stageRulings(batch, runId);
		}

		final int staged = rulingImportDao.countStaged(runId);
		if (failed > 0) {
			log.warn("Ruling mirror skipped: {} ruling rows failed", failed);
			return new RulingCounts(staged, 0, 0, skipped, failed, true);
		}
		if (staged == 0) {
			throw new IllegalStateException(
					"Rulings file produced no rows; refusing to mirror the table");
		}

		final RulingImportDao.RulingApplyResult applied = Objects.requireNonNull(
			transactionTemplate.execute(status -> rulingImportDao.apply(runId)),
			"Ruling mirror returned no result");
		return new RulingCounts(staged, applied.inserted(), applied.deleted(), skipped, failed, false);
	}

	private int stageRulings(List<ImportRuling> batch, UUID runId) {
		try {
			rulingImportDao.stage(batch, runId);
			return 0;
		}
		catch (RuntimeException batchFailure) {
			log.warn("Ruling batch of {} failed, retrying row by row: {}",
					batch.size(), batchFailure.getMessage());
			int failed = 0;
			for (final ImportRuling ruling : batch) {
				try {
					rulingImportDao.stage(ruling, runId);
				}
				catch (RuntimeException rowFailure) {
					failed++;
					log.warn("Ruling for {} failed: {}", ruling.oracleId(), rowFailure.getMessage());
				}
			}
			return failed;
		}
	}

	private void clearRun(UUID runId) {
		try {
			cardImportDao.clearSeen(runId);
		}
		catch (RuntimeException ex) {
			log.warn("Could not clear import_seen for run {}: {}", runId, ex.getMessage());
		}
		try {
			rulingImportDao.clearStaged(runId);
		}
		catch (RuntimeException ex) {
			log.warn("Could not clear import_ruling for run {}: {}", runId, ex.getMessage());
		}
	}

	private static boolean limitReached(ImportOptions options, int processed) {
		return options.getLimit() != null && processed >= options.getLimit();
	}

	private record CardCounts(int inserted, int updated, int skipped, int failed, int obsolete,
			boolean sweepSkipped) {
	}

	private record RulingCounts(int staged, int inserted, int deleted, int skipped, int failed,
			boolean mirrorSkipped) {

		static RulingCounts none(boolean mirrorSkipped) {
			return new RulingCounts(0, 0, 0, 0, 0, mirrorSkipped);
		}
	}

	private record BatchTotals(int inserted, int updated, int failed) {
	}
}
