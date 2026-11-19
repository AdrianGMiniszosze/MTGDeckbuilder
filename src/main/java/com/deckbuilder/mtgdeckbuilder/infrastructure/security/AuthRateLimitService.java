package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.RateLimitStoreUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Atomic fixed-window limiter backed by PostgreSQL. A shared database makes
 * limits consistent across application replicas.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthRateLimitService {

	static final String CONSUME_SQL = """
			INSERT INTO auth_rate_limit_buckets
				(bucket_key, window_start_epoch, request_count, updated_at)
			VALUES (?, ?, 1, CURRENT_TIMESTAMP)
			ON CONFLICT (bucket_key) DO UPDATE SET
				window_start_epoch = EXCLUDED.window_start_epoch,
				request_count = CASE
					WHEN auth_rate_limit_buckets.window_start_epoch = EXCLUDED.window_start_epoch
					THEN auth_rate_limit_buckets.request_count + 1
					ELSE 1
				END,
				updated_at = CURRENT_TIMESTAMP
			RETURNING request_count
			""";

	private final JdbcTemplate jdbcTemplate;
	private final JwtProperties jwtProperties;
	private final AuthRateLimitMetrics metrics;
	private final Clock clock;

	public AuthRateLimitDecision consume(AuthRateLimitEndpoint endpoint, String clientAddress,
			AuthRateLimitProperties.Rule rule) {
		final long now = Instant.now(this.clock).getEpochSecond();
		final long windowSeconds = rule.window().toSeconds();
		final long windowStart = now / windowSeconds * windowSeconds;
		final long count;
		try {
			count = this.jdbcTemplate.queryForObject(CONSUME_SQL, Long.class,
					bucketKey(endpoint, clientAddress), windowStart);
		} catch (DataAccessException exception) {
			this.metrics.storeUnavailable(endpoint);
			throw new RateLimitStoreUnavailableException();
		}

		final boolean allowed = count <= rule.maxRequests();
		final long retryAfter = allowed ? 0 : Math.max(1, windowStart + windowSeconds - now);
		this.metrics.record(endpoint, allowed);
		return new AuthRateLimitDecision(allowed, retryAfter, count);
	}

	private String bucketKey(AuthRateLimitEndpoint endpoint, String clientAddress) {
		try {
			final Mac mac = Mac.getInstance("HmacSHA256");
			final byte[] secret = this.jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			final String identity = "mtgdeckbuilder:auth-rate-limit:" + endpoint.metricName() + ":"
					+ (clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress);
			return HexFormat.of().formatHex(mac.doFinal(identity.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("HmacSHA256 is unavailable", exception);
		}
	}
}
