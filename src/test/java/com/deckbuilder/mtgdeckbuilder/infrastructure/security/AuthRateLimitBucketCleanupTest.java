package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Auth Rate Limit Bucket Cleanup Tests")
class AuthRateLimitBucketCleanupTest {

	@Test
	@DisplayName("Should delete buckets older than the configured retention")
	void shouldDeleteExpiredBuckets() {
		final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
		when(jdbcTemplate.update(eq("DELETE FROM auth_rate_limit_buckets WHERE updated_at < ?"),
				any(java.sql.Timestamp.class))).thenReturn(3);
		final AuthRateLimitProperties properties = new AuthRateLimitProperties();
		properties.setBucketRetention(Duration.ofHours(12));
		final Instant now = Instant.parse("2026-01-01T12:00:00Z");
		final AuthRateLimitBucketCleanup cleanup = new AuthRateLimitBucketCleanup(jdbcTemplate, properties,
				Clock.fixed(now, ZoneOffset.UTC));

		cleanup.removeExpiredBuckets();

		verify(jdbcTemplate).update("DELETE FROM auth_rate_limit_buckets WHERE updated_at < ?",
				java.sql.Timestamp.from(now.minus(Duration.ofHours(12))));
	}
}
