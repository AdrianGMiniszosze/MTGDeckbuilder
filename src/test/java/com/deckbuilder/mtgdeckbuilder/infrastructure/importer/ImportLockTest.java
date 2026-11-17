package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Import advisory lock")
class ImportLockTest {

	private final DataSource dataSource = mock(DataSource.class);
	private final Connection connection = mock(Connection.class);

	@Test
	@DisplayName("Should release the lock only once and close its own connection")
	void shouldReleaseOnce() throws Exception {
		final PreparedStatement queryStatement = mock(PreparedStatement.class);
		final PreparedStatement unlockStatement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString()))
			.thenReturn(queryStatement)
			.thenReturn(unlockStatement);
		when(queryStatement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getBoolean(1)).thenReturn(true);

		final ImportLock lock = ImportLock.tryAcquire(dataSource).orElseThrow();
		lock.close();
		lock.close();

		verify(resultSet).close();
		verify(queryStatement).close();
		verify(unlockStatement).execute();
		verify(unlockStatement).close();
		verify(connection).close();
	}

	@Test
	@DisplayName("Should return empty when another session holds the lock")
	void shouldReturnEmptyWhenBusy() throws Exception {
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getBoolean(1)).thenReturn(false);

		final Optional<ImportLock> lock = ImportLock.tryAcquire(dataSource);

		assertThat(lock).isEmpty();
		verify(connection).close();
	}

	@Test
	@DisplayName("Should return empty when the lock query yields no row")
	void shouldReturnEmptyWithoutRow() throws Exception {
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);

		assertThat(ImportLock.tryAcquire(dataSource)).isEmpty();
		verify(connection).close();
	}

	@Test
	@DisplayName("Should close the connection when the lock query fails")
	void shouldCloseConnectionOnFailure() throws Exception {
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

		assertThatThrownBy(() -> ImportLock.tryAcquire(dataSource))
			.isInstanceOf(SQLException.class)
			.hasMessage("boom");

		verify(connection).close();
	}

	@Test
	@DisplayName("Should close only once even after a failed release")
	void shouldCloseEvenIfUnlockFails() throws Exception {
		final PreparedStatement queryStatement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		final PreparedStatement unlockStatement = mock(PreparedStatement.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString()))
			.thenReturn(queryStatement)
			.thenReturn(unlockStatement);
		when(queryStatement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getBoolean(1)).thenReturn(true);
		when(unlockStatement.execute()).thenThrow(new SQLException("unlock failed"));

		final ImportLock lock = ImportLock.tryAcquire(dataSource).orElseThrow();

		assertThatThrownBy(lock::close).isInstanceOf(SQLException.class);
		verify(connection).close();
	}
}
