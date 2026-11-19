package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.RateLimitStoreUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.Provider;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Rate Limit Service Tests")
class AuthRateLimitServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T00:02:03Z");

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private AuthRateLimitMetrics metrics;

	private JwtProperties jwtProperties;

	private AuthRateLimitService service;

	@BeforeEach
	void setUp() {
		this.jwtProperties = new JwtProperties();
		this.jwtProperties.setSecret("rate-limit-test-secret-0123456789abcdef");
		this.service = new AuthRateLimitService(this.jdbcTemplate, this.jwtProperties, this.metrics,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	@DisplayName("Should allow requests through the configured limit")
	void shouldAllowRequestWithinLimit() {
		when(this.jdbcTemplate.queryForObject(eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class), any(Object[].class)))
				.thenReturn(20L);

		final AuthRateLimitDecision result = this.service.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.1",
				new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5)));

		assertThat(result).isEqualTo(new AuthRateLimitDecision(true, 0, 20));
		final ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
		verify(this.jdbcTemplate).queryForObject(eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class),
				arguments.capture());
		assertThat(arguments.getValue()[0].toString()).isNotEqualTo("192.0.2.1").hasSize(64);
		assertThat(arguments.getValue()[1]).isEqualTo(1767225600L);
		verify(this.metrics).record(AuthRateLimitEndpoint.LOGIN, true);
	}

	@Test
	@DisplayName("Should reject above the limit and return the window retry delay")
	void shouldRejectAboveLimit() {
		when(this.jdbcTemplate.queryForObject(eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class), any(Object[].class)))
				.thenReturn(21L);

		final AuthRateLimitDecision result = this.service.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.2",
				new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5)));

		assertThat(result).isEqualTo(new AuthRateLimitDecision(false, 177, 21));
		verify(this.metrics).record(AuthRateLimitEndpoint.LOGIN, false);
	}

	@Test
	@DisplayName("Should use a separate HMAC bucket for each endpoint and peer")
	void shouldSeparateBuckets() {
		when(this.jdbcTemplate.queryForObject(eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class), any(Object[].class)))
				.thenReturn(1L, 1L);

		this.service.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.3",
				new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5)));
		this.service.consume(AuthRateLimitEndpoint.REGISTER, "192.0.2.3",
				new AuthRateLimitProperties.Rule(10, Duration.ofHours(1)));

		final ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
		verify(this.jdbcTemplate, org.mockito.Mockito.times(2)).queryForObject(
				eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class), arguments.capture());
		assertThat(arguments.getAllValues().get(0)[0]).isNotEqualTo(arguments.getAllValues().get(1)[0]);
		verify(this.metrics).record(AuthRateLimitEndpoint.LOGIN, true);
		verify(this.metrics).record(AuthRateLimitEndpoint.REGISTER, true);
	}

	@Test
	@DisplayName("Should coalesce absent or blank peer addresses without storing them")
	void shouldUseUnknownAddressForMissingPeer() {
		when(this.jdbcTemplate.queryForObject(eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class), any(Object[].class)))
				.thenReturn(1L, 1L);
		final AuthRateLimitProperties.Rule rule = new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5));

		this.service.consume(AuthRateLimitEndpoint.LOGIN, null, rule);
		this.service.consume(AuthRateLimitEndpoint.LOGIN, "  ", rule);

		final ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
		verify(this.jdbcTemplate, org.mockito.Mockito.times(2)).queryForObject(
				eq(AuthRateLimitService.CONSUME_SQL), eq(Long.class), arguments.capture());
		assertThat(arguments.getAllValues().get(0)[0]).isEqualTo(arguments.getAllValues().get(1)[0]);
	}

	@Test
	@DisplayName("Should fail closed when the shared database cannot consume a bucket")
	void shouldFailClosedWhenStoreIsUnavailable() {
		when(this.jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
				.thenThrow(new DataAccessResourceFailureException("database unavailable"));

		assertThatThrownBy(() -> this.service.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.4",
				new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5))))
				.isInstanceOf(RateLimitStoreUnavailableException.class)
				.hasMessage("Authentication rate-limit service is temporarily unavailable");
		verify(this.metrics).storeUnavailable(AuthRateLimitEndpoint.LOGIN);
		verify(this.metrics, never()).record(any(), anyBoolean());
	}

	@Test
	@DisplayName("Should fail closed if HMAC-SHA256 is unavailable")
	void shouldFailIfHmacIsUnavailable() {
		final Provider[] providers = Security.getProviders();
		try {
			for (Provider provider : providers) {
				Security.removeProvider(provider.getName());
			}
			assertThatThrownBy(() -> this.service.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.5",
					new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5))))
					.isInstanceOf(IllegalStateException.class).hasMessage("HmacSHA256 is unavailable");
		} finally {
			for (int i = 0; i < providers.length; i++) {
				Security.insertProviderAt(providers[i], i + 1);
			}
		}
		verify(this.jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), any(Object[].class));
	}
}
