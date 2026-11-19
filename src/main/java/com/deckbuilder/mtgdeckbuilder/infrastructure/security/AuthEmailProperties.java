package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.security.email")
@Data
public class AuthEmailProperties {

	private String from = "no-reply@mtgdeckbuilder.local";
	private String frontendBaseUrl = "http://localhost:3000";
	private String verificationPath = "/verify-email";
	private String passwordResetPath = "/reset-password";
	private Duration verificationTokenTtl = Duration.ofHours(24);
	private Duration passwordResetTokenTtl = Duration.ofHours(1);

	@PostConstruct
	void validate() {
		if (this.from == null || this.from.isBlank() || this.frontendBaseUrl == null
				|| this.frontendBaseUrl.isBlank()) {
			throw new IllegalArgumentException("Auth email sender and frontend base URL are required");
		}
		validateTtl(this.verificationTokenTtl);
		validateTtl(this.passwordResetTokenTtl);
	}

	private static void validateTtl(Duration ttl) {
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			throw new IllegalArgumentException("Auth email token lifetime must be positive");
		}
	}
}
