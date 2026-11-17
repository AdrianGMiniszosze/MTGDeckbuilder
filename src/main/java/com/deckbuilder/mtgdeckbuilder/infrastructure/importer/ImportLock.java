package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Session-level PostgreSQL advisory lock that keeps two imports from running
 * at once.
 *
 * <p>
 * The lock must live on one retained connection: {@code JdbcTemplate} borrows a
 * connection per statement from the pool, so acquiring and releasing with it
 * could hit different sessions. Closing the connection releases the lock even
 * if the explicit unlock fails.
 *
 * <p>
 * Acquisition is non-blocking ({@code pg_try_advisory_lock}); a second importer
 * gets an empty result and exits with code 2 (D-031).
 */
public final class ImportLock implements AutoCloseable {

	/**
	 * Fixed application-wide key ("MTGDLCK" in ASCII hex). It only has to be
	 * unique within this database; document it before reusing another process.
	 */
	static final long LOCK_KEY = 0x4D5447444C4F434BL;

	private static final String TRY_LOCK = "SELECT pg_try_advisory_lock(?)";
	private static final String UNLOCK = "SELECT pg_advisory_unlock(?)";

	private final Connection connection;
	private boolean released;

	private ImportLock(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Tries to take the import lock without waiting.
	 * @return the held lock, or empty when another session already has it
	 */
	public static Optional<ImportLock> tryAcquire(DataSource dataSource) throws SQLException {
		final Connection connection = dataSource.getConnection();
		try {
			if (!tryLock(connection)) {
				connection.close();
				return Optional.empty();
			}
			return Optional.of(new ImportLock(connection));
		}
		catch (SQLException ex) {
			connection.close();
			throw ex;
		}
	}

	private static boolean tryLock(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(TRY_LOCK)) {
			statement.setLong(1, LOCK_KEY);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getBoolean(1);
			}
		}
	}

	@Override
	public void close() throws SQLException {
		if (released) {
			return;
		}
		released = true;
		try {
			try (PreparedStatement statement = connection.prepareStatement(UNLOCK)) {
				statement.setLong(1, LOCK_KEY);
				statement.execute();
			}
		}
		finally {
			connection.close();
		}
	}
}
