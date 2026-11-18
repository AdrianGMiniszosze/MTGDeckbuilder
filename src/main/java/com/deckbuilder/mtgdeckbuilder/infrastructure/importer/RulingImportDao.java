package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import com.deckbuilder.mtgdeckbuilder.application.importing.ImportRuling;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Mirrors {@code card_rulings} from the current Scryfall rulings bulk.
 *
 * <p>
 * Scryfall has no ruling id, so a correction cannot be matched with the old
 * row. Instead every ruling of the run is staged in {@code import_ruling} and
 * the apply step deletes the rows that are not in that staging table and
 * inserts the new ones. A corrected or retired ruling simply disappears.
 *
 * <p>
 * The table is UNLOGGED and shared by the pool, so the caller must clean its
 * own run and stale runs (same pattern as {@code import_seen}).
 */
@Repository
public class RulingImportDao {

	private static final String INSERT_STAGED = """
			INSERT INTO import_ruling (run_id, oracle_id, published_at, comment, source)
			VALUES (?, ?, ?, ?, ?)
			ON CONFLICT DO NOTHING
			""";

	private static final String DELETE_STALE = """
			DELETE FROM card_rulings cr
			 WHERE NOT EXISTS (
			     SELECT 1 FROM import_ruling s
			      WHERE s.run_id = ?
			        AND s.oracle_id = cr.oracle_id
			        AND s.published_at = cr.published_at
			        AND s.comment = cr.comment
			        AND s.source = cr.source)
			""";

	private static final String INSERT_CURRENT = """
			INSERT INTO card_rulings (oracle_id, published_at, comment, source)
			SELECT oracle_id, published_at, comment, source
			  FROM import_ruling
			 WHERE run_id = ?
			ON CONFLICT DO NOTHING
			""";

	private static final String COUNT_STAGED = "SELECT COUNT(*) FROM import_ruling WHERE run_id = ?";
	private static final String DELETE_STAGED = "DELETE FROM import_ruling WHERE run_id = ?";
	private static final String DELETE_OTHER_STAGED = "DELETE FROM import_ruling WHERE run_id <> ?";

	private static final int BATCH_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;

	public RulingImportDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void stage(List<ImportRuling> rulings, UUID runId) {
		jdbcTemplate.batchUpdate(INSERT_STAGED, rulings, BATCH_SIZE, (statement, ruling) -> {
			statement.setObject(1, runId);
			statement.setObject(2, ruling.oracleId());
			statement.setObject(3, ruling.publishedAt());
			statement.setString(4, ruling.comment());
			statement.setString(5, ruling.source());
		});
	}

	public void stage(ImportRuling ruling, UUID runId) {
		jdbcTemplate.update(INSERT_STAGED,
				runId, ruling.oracleId(), ruling.publishedAt(), ruling.comment(), ruling.source());
	}

	public int countStaged(UUID runId) {
		final Integer count = jdbcTemplate.queryForObject(COUNT_STAGED, Integer.class, runId);
		return count == null ? 0 : count;
	}

	/**
	 * Deletes rulings that are not in the current run and inserts the staged ones.
	 * @throws IllegalStateException when the run staged nothing, because an empty
	 * or truncated file must not wipe every ruling.
	 */
	public RulingApplyResult apply(UUID runId) {
		if (countStaged(runId) == 0) {
			throw new IllegalStateException(
					"Refusing to mirror rulings: no staged rows for run " + runId);
		}
		final int deleted = jdbcTemplate.update(DELETE_STALE, runId);
		final int inserted = jdbcTemplate.update(INSERT_CURRENT, runId);
		return new RulingApplyResult(deleted, inserted);
	}

	public int clearStaged(UUID runId) {
		return jdbcTemplate.update(DELETE_STAGED, runId);
	}

	public int clearOtherRuns(UUID runId) {
		return jdbcTemplate.update(DELETE_OTHER_STAGED, runId);
	}

	public record RulingApplyResult(int deleted, int inserted) {
	}
}
