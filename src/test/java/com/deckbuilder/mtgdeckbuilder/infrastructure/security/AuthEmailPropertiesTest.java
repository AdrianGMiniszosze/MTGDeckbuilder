package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Auth Email Properties Tests")
class AuthEmailPropertiesTest {

	@Test
	@DisplayName("Should provide local defaults and validate them")
	void shouldValidateDefaults() {
		final AuthEmailProperties properties = new AuthEmailProperties();

		properties.validate();

		assertThat(properties.getFrom()).isEqualTo("no-reply@mtgdeckbuilder.local");
		assertThat(properties.getFrontendBaseUrl()).isEqualTo("http://localhost:3000");
		assertThat(properties.getVerificationTokenTtl()).isEqualTo(Duration.ofHours(24));
		assertThat(properties.getPasswordResetTokenTtl()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	@DisplayName("Should require sender and frontend base URL")
	void shouldRequireSenderAndBaseUrl() {
		final AuthEmailProperties properties = new AuthEmailProperties();
		properties.setFrom(null);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);

		properties.setFrom(" ");
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Auth email sender and frontend base URL are required");

		properties.setFrom("no-reply@example.com");
		properties.setFrontendBaseUrl(null);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);
		properties.setFrontendBaseUrl(" ");
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Should require positive token lifetimes")
	void shouldRequirePositiveLifetimes() {
		final AuthEmailProperties properties = new AuthEmailProperties();
		properties.setVerificationTokenTtl(null);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Auth email token lifetime must be positive");

		properties.setVerificationTokenTtl(Duration.ofHours(1));
		properties.setPasswordResetTokenTtl(Duration.ofSeconds(-1));
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);

		properties.setPasswordResetTokenTtl(Duration.ZERO);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);
	}
}
