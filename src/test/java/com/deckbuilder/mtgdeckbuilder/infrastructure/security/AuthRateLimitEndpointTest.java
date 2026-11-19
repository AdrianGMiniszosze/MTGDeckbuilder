package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth Rate Limit Endpoint Tests")
class AuthRateLimitEndpointTest {

	@Test
	@DisplayName("Should resolve each public auth endpoint")
	void shouldResolveKnownRoutes() {
		assertThat(AuthRateLimitEndpoint.resolve("POST", "/auth/login"))
				.contains(AuthRateLimitEndpoint.LOGIN);
		assertThat(AuthRateLimitEndpoint.resolve("POST", "/auth/register"))
				.contains(AuthRateLimitEndpoint.REGISTER);
		assertThat(AuthRateLimitEndpoint.resolve("POST", "/auth/refresh"))
				.contains(AuthRateLimitEndpoint.REFRESH);
		assertThat(AuthRateLimitEndpoint.resolve("POST", "/auth/logout"))
				.contains(AuthRateLimitEndpoint.LOGOUT);
		assertThat(AuthRateLimitEndpoint.LOGIN.metricName()).isEqualTo("login");
	}

	@Test
	@DisplayName("Should ignore other methods and paths")
	void shouldIgnoreUnknownRoutes() {
		assertThat(AuthRateLimitEndpoint.resolve("GET", "/auth/login")).isEmpty();
		assertThat(AuthRateLimitEndpoint.resolve("POST", "/auth/me")).isEmpty();
		assertThat(AuthRateLimitEndpoint.resolve(null, "/auth/login")).isEmpty();
		assertThat(AuthRateLimitEndpoint.resolve("POST", null)).isEmpty();
	}
}
