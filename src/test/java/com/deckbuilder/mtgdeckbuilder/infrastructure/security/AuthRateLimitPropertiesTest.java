package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Auth Rate Limit Properties Tests")
class AuthRateLimitPropertiesTest {

	@Test
	@DisplayName("Should provide conservative endpoint-specific defaults")
	void shouldProvideEndpointDefaults() {
		final AuthRateLimitProperties properties = new AuthRateLimitProperties();

		assertThat(properties.isEnabled()).isTrue();
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.LOGIN))
				.isEqualTo(new AuthRateLimitProperties.Rule(20, Duration.ofMinutes(5)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.REGISTER))
				.isEqualTo(new AuthRateLimitProperties.Rule(10, Duration.ofHours(1)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.REFRESH))
				.isEqualTo(new AuthRateLimitProperties.Rule(60, Duration.ofMinutes(5)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.LOGOUT))
				.isEqualTo(new AuthRateLimitProperties.Rule(60, Duration.ofMinutes(5)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.EMAIL_VERIFY))
				.isEqualTo(new AuthRateLimitProperties.Rule(20, Duration.ofHours(1)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.EMAIL_RESEND))
				.isEqualTo(new AuthRateLimitProperties.Rule(5, Duration.ofHours(1)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.PASSWORD_RESET_REQUEST))
				.isEqualTo(new AuthRateLimitProperties.Rule(5, Duration.ofHours(1)));
		assertThat(properties.ruleFor(AuthRateLimitEndpoint.PASSWORD_RESET))
				.isEqualTo(new AuthRateLimitProperties.Rule(10, Duration.ofHours(1)));
	}

	@Test
	@DisplayName("Should reject a zero or negative request limit")
	void shouldRejectInvalidRequestLimit() {
		assertThatThrownBy(() -> new AuthRateLimitProperties.Rule(0, Duration.ofMinutes(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Rate-limit maxRequests must be positive");
		assertThatThrownBy(() -> new AuthRateLimitProperties.Rule(-1, Duration.ofMinutes(1)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Should reject an absent or non-positive window")
	void shouldRejectInvalidWindow() {
		assertThatThrownBy(() -> new AuthRateLimitProperties.Rule(1, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Rate-limit window must be at least one second");
		assertThatThrownBy(() -> new AuthRateLimitProperties.Rule(1, Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AuthRateLimitProperties.Rule(1, Duration.ofMillis(500)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AuthRateLimitProperties.Rule(1, Duration.ofSeconds(-1)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Should validate all endpoint limits and bucket retention")
	void shouldValidateConfiguration() {
		final AuthRateLimitProperties properties = new AuthRateLimitProperties();
		properties.validate();

		properties.setLoginLimit(0);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Rate-limit maxRequests must be positive");

		properties.setLoginLimit(20);
		properties.setBucketRetention(null);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Rate-limit bucket retention must be positive");

		properties.setBucketRetention(Duration.ofSeconds(-1));
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Rate-limit bucket retention must be positive");

		properties.setBucketRetention(Duration.ZERO);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Rate-limit bucket retention must be positive");
	}
}
