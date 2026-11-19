package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Per-peer throttles for public authentication endpoints. Limits count all
 * requests, including valid requests, to keep enforcement before body parsing.
 */
@Component
@ConfigurationProperties(prefix = "app.security.rate-limit")
@Data
public class AuthRateLimitProperties {

	private boolean enabled = true;

	private int loginLimit = 20;
	private Duration loginWindow = Duration.ofMinutes(5);

	private int registerLimit = 10;
	private Duration registerWindow = Duration.ofHours(1);

	private int refreshLimit = 60;
	private Duration refreshWindow = Duration.ofMinutes(5);

	private int logoutLimit = 60;
	private Duration logoutWindow = Duration.ofMinutes(5);

	private Duration bucketRetention = Duration.ofDays(1);

	@PostConstruct
	void validate() {
		for (AuthRateLimitEndpoint endpoint : AuthRateLimitEndpoint.values()) {
			ruleFor(endpoint);
		}
		if (this.bucketRetention == null || this.bucketRetention.isNegative()
				|| this.bucketRetention.isZero()) {
			throw new IllegalArgumentException("Rate-limit bucket retention must be positive");
		}
	}

	public Rule ruleFor(AuthRateLimitEndpoint endpoint) {
		return switch (endpoint) {
			case LOGIN -> new Rule(this.loginLimit, this.loginWindow);
			case REGISTER -> new Rule(this.registerLimit, this.registerWindow);
			case REFRESH -> new Rule(this.refreshLimit, this.refreshWindow);
			case LOGOUT -> new Rule(this.logoutLimit, this.logoutWindow);
		};
	}

	public record Rule(int maxRequests, Duration window) {
		public Rule {
			if (maxRequests < 1) {
				throw new IllegalArgumentException("Rate-limit maxRequests must be positive");
			}
			if (window == null || window.isNegative() || window.isZero() || window.toSeconds() < 1) {
				throw new IllegalArgumentException("Rate-limit window must be at least one second");
			}
		}
	}
}
