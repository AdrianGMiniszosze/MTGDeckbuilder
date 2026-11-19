package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import java.util.Arrays;
import java.util.Optional;

/**
 * The small, explicit set of public authentication routes that consume a
 * rate-limit bucket.
 */
public enum AuthRateLimitEndpoint {
	LOGIN("POST", "/auth/login", "login"),
	REGISTER("POST", "/auth/register", "register"),
	REFRESH("POST", "/auth/refresh", "refresh"),
	LOGOUT("POST", "/auth/logout", "logout"),
	EMAIL_VERIFY("POST", "/auth/verify-email", "email_verify"),
	EMAIL_RESEND("POST", "/auth/verify-email/resend", "email_resend"),
	PASSWORD_RESET_REQUEST("POST", "/auth/password-reset/request", "password_reset_request"),
	PASSWORD_RESET("POST", "/auth/password-reset", "password_reset");

	private final String method;
	private final String path;
	private final String metricName;

	AuthRateLimitEndpoint(String method, String path, String metricName) {
		this.method = method;
		this.path = path;
		this.metricName = metricName;
	}

	public String metricName() {
		return this.metricName;
	}

	public static Optional<AuthRateLimitEndpoint> resolve(String method, String path) {
		if (method == null || path == null) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(endpoint -> endpoint.method.equalsIgnoreCase(method) && endpoint.path.equals(path))
				.findFirst();
	}
}
