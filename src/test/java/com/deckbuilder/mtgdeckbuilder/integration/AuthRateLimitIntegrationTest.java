package com.deckbuilder.mtgdeckbuilder.integration;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentication Rate Limit Integration Tests")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"app.security.rate-limit.enabled=true",
		"app.security.rate-limit.login-limit=2",
		"app.security.rate-limit.login-window=PT1M",
		"app.security.rate-limit.register-limit=1",
		"app.security.rate-limit.register-window=PT1M",
		"app.security.rate-limit.refresh-limit=2",
		"app.security.rate-limit.refresh-window=PT1M",
		"app.security.rate-limit.logout-limit=2",
		"app.security.rate-limit.logout-window=PT1M",
		"app.security.rate-limit.email-verify-limit=1",
		"app.security.rate-limit.email-verify-window=PT1M",
		"app.security.rate-limit.email-resend-limit=1",
		"app.security.rate-limit.email-resend-window=PT1M",
		"app.security.rate-limit.password-reset-request-limit=1",
		"app.security.rate-limit.password-reset-request-window=PT1M",
		"app.security.rate-limit.password-reset-limit=1",
		"app.security.rate-limit.password-reset-window=PT1M"
})
class AuthRateLimitIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeterRegistry meterRegistry;

	@Test
	@DisplayName("Should throttle login with 429 and Retry-After without exposing identity")
	void shouldThrottleLogin() throws Exception {
		final String address = "198.51.100.71";
		login(address).andExpect(status().isUnauthorized());
		login(address).andExpect(status().isUnauthorized());
		login(address).andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
				.andExpect(jsonPath("$.message").value("Too many authentication requests. Retry later."));

		assertThat(this.meterRegistry.get("auth.rate_limit.rejected").tag("endpoint", "login").counter().count())
				.isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("Should keep buckets separate by peer address")
	void shouldSeparatePeerAddresses() throws Exception {
		final String exhausted = "198.51.100.72";
		login(exhausted).andExpect(status().isUnauthorized());
		login(exhausted).andExpect(status().isUnauthorized());
		login(exhausted).andExpect(status().isTooManyRequests());

		login("198.51.100.73").andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Should isolate route buckets")
	void shouldIsolateEndpointBuckets() throws Exception {
		final String address = "198.51.100.74";

		this.mockMvc.perform(post("/auth/register").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
		this.mockMvc.perform(post("/auth/register").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isTooManyRequests());

		login(address).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Should throttle refresh and logout independently")
	void shouldThrottleRefreshAndLogout() throws Exception {
		final String refreshAddress = "198.51.100.75";
		final String refreshBody = "{\"refresh_token\":\"unknown-refresh-token\"}";
		refresh(refreshAddress, refreshBody).andExpect(status().isUnauthorized());
		refresh(refreshAddress, refreshBody).andExpect(status().isUnauthorized());
		refresh(refreshAddress, refreshBody).andExpect(status().isTooManyRequests());

		final String logoutAddress = "198.51.100.76";
		logout(logoutAddress, refreshBody).andExpect(status().isNoContent());
		logout(logoutAddress, refreshBody).andExpect(status().isNoContent());
		logout(logoutAddress, refreshBody).andExpect(status().isTooManyRequests());
	}

	@Test
	@DisplayName("Should throttle verification and password recovery routes")
	void shouldThrottleAccountActionRoutes() throws Exception {
		final String verifyAddress = "198.51.100.77";
		verifyEmail(verifyAddress, "{\"verification_token\":\"bad-token\"}")
				.andExpect(status().isBadRequest());
		verifyEmail(verifyAddress, "{\"verification_token\":\"bad-token\"}")
				.andExpect(status().isTooManyRequests());

		final String resendAddress = "198.51.100.78";
		emailRequest("/auth/verify-email/resend", resendAddress, "missing@example.com")
				.andExpect(status().isAccepted());
		emailRequest("/auth/verify-email/resend", resendAddress, "missing@example.com")
				.andExpect(status().isTooManyRequests());

		final String requestAddress = "198.51.100.79";
		emailRequest("/auth/password-reset/request", requestAddress, "missing@example.com")
				.andExpect(status().isAccepted());
		emailRequest("/auth/password-reset/request", requestAddress, "missing@example.com")
				.andExpect(status().isTooManyRequests());

		final String resetAddress = "198.51.100.80";
		resetPassword(resetAddress, "{\"reset_token\":\"bad-token\",\"new_password\":\"new-secret-123\"}")
				.andExpect(status().isBadRequest());
		resetPassword(resetAddress, "{\"reset_token\":\"bad-token\",\"new_password\":\"new-secret-123\"}")
				.andExpect(status().isTooManyRequests());
	}

	private org.springframework.test.web.servlet.ResultActions login(String address) throws Exception {
		return this.mockMvc.perform(post("/auth/login").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"unknown-user\",\"password\":\"wrong-password\"}"));
	}

	private org.springframework.test.web.servlet.ResultActions refresh(String address, String body) throws Exception {
		return this.mockMvc.perform(post("/auth/refresh").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions logout(String address, String body) throws Exception {
		return this.mockMvc.perform(post("/auth/logout").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions verifyEmail(String address, String body)
			throws Exception {
		return this.mockMvc.perform(post("/auth/verify-email").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions emailRequest(String path, String address, String email)
			throws Exception {
		return this.mockMvc.perform(post(path).with(remoteAddress(address)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\"}"));
	}

	private org.springframework.test.web.servlet.ResultActions resetPassword(String address, String body)
			throws Exception {
		return this.mockMvc.perform(post("/auth/password-reset").with(remoteAddress(address))
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static RequestPostProcessor remoteAddress(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
