package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth Email Delivery Metrics Tests")
class AuthEmailDeliveryMetricsTest {

	@Test
	@DisplayName("Should count delivery failures with purpose-only tags")
	void shouldCountFailures() {
		final SimpleMeterRegistry registry = new SimpleMeterRegistry();
		final AuthEmailDeliveryMetrics metrics = new AuthEmailDeliveryMetrics(registry);

		metrics.deliveryFailed(AccountActionPurpose.EMAIL_VERIFICATION);
		metrics.deliveryFailed(AccountActionPurpose.PASSWORD_RESET);

		assertThat(registry.get("auth.email.delivery.failure").tag("purpose", "email_verification").counter()
				.count()).isEqualTo(1);
		assertThat(registry.get("auth.email.delivery.failure").tag("purpose", "password_reset").counter()
				.count()).isEqualTo(1);
		assertThat(registry.getMeters()).hasSize(2);
		registry.close();
	}
}
