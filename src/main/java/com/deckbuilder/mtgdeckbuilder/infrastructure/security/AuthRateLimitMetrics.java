package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

/**
 * Low-cardinality counters. No client address, username or token is used as a
 * metric tag.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthRateLimitMetrics {

	private final MeterRegistry meterRegistry;

	public void record(AuthRateLimitEndpoint endpoint, boolean allowed) {
		final String metric = allowed ? "auth.rate_limit.allowed" : "auth.rate_limit.rejected";
		this.meterRegistry.counter(metric, "endpoint", endpoint.metricName()).increment();
	}

	public void storeUnavailable(AuthRateLimitEndpoint endpoint) {
		this.meterRegistry.counter("auth.rate_limit.store_unavailable", "endpoint", endpoint.metricName())
				.increment();
	}
}
