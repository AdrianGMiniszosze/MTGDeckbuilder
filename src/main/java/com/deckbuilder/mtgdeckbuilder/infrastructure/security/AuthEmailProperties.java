package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
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
		final URI frontend;
		try {
			frontend = URI.create(this.frontendBaseUrl);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Auth email frontend base URL must be absolute HTTP(S)", exception);
		}
		if (!("http".equalsIgnoreCase(frontend.getScheme()) || "https".equalsIgnoreCase(frontend.getScheme()))
				|| frontend.getHost() == null || frontend.getRawUserInfo() != null
				|| frontend.getRawQuery() != null || frontend.getRawFragment() != null) {
			throw new IllegalArgumentException("Auth email frontend base URL must be absolute HTTP(S)");
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
