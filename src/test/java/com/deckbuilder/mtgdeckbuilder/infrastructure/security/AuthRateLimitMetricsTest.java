package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth Rate Limit Metrics Tests")
class AuthRateLimitMetricsTest {

	@Test
	@DisplayName("Should record only low-cardinality endpoint counters")
	void shouldRecordEndpointCounters() {
		final SimpleMeterRegistry registry = new SimpleMeterRegistry();
		final AuthRateLimitMetrics metrics = new AuthRateLimitMetrics(registry);

		metrics.record(AuthRateLimitEndpoint.LOGIN, true);
		metrics.record(AuthRateLimitEndpoint.LOGIN, false);
		metrics.storeUnavailable(AuthRateLimitEndpoint.REFRESH);

		assertThat(registry.get("auth.rate_limit.allowed").tag("endpoint", "login").counter().count())
				.isEqualTo(1);
		assertThat(registry.get("auth.rate_limit.rejected").tag("endpoint", "login").counter().count())
				.isEqualTo(1);
		assertThat(registry.get("auth.rate_limit.store_unavailable").tag("endpoint", "refresh").counter().count())
				.isEqualTo(1);
		assertThat(registry.getMeters()).hasSize(3);
		registry.close();
	}
}
