package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthRateLimitBucketCleanup {

	private final JdbcTemplate jdbcTemplate;
	private final AuthRateLimitProperties properties;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${app.security.rate-limit.cleanup-delay:PT1H}")
	public void removeExpiredBuckets() {
		final Instant cutoff = Instant.now(this.clock).minus(this.properties.getBucketRetention());
		this.jdbcTemplate.update("DELETE FROM auth_rate_limit_buckets WHERE updated_at < ?",
				Timestamp.from(cutoff));
	}
}
