package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Rate Limit Interceptor Tests")
class AuthRateLimitInterceptorTest {

	@Mock
	private AuthRateLimitService rateLimitService;

	@Mock
	private AuthRateLimitProperties properties;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private AuthRateLimitInterceptor interceptor;

	@Test
	@DisplayName("Should pass a matching auth request through the shared limiter")
	void shouldLimitKnownAuthRoute() {
		when(this.properties.isEnabled()).thenReturn(true);
		final AuthRateLimitProperties.Rule rule = new AuthRateLimitProperties.Rule(20,
				java.time.Duration.ofMinutes(5));
		when(this.request.getServletPath()).thenReturn("/auth/login");
		when(this.request.getMethod()).thenReturn("POST");
		when(this.request.getRemoteAddr()).thenReturn("192.0.2.20");
		when(this.rateLimitService.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.20", rule))
				.thenReturn(new AuthRateLimitDecision(true, 0, 1));
		when(this.properties.ruleFor(AuthRateLimitEndpoint.LOGIN)).thenReturn(rule);

		assertThat(this.interceptor.preHandle(this.request, null, new Object())).isTrue();
		verify(this.rateLimitService).consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.20", rule);
	}

	@Test
	@DisplayName("Should return true when throttling is disabled")
	void shouldSkipWhenDisabled() {
		when(this.properties.isEnabled()).thenReturn(false);

		assertThat(this.interceptor.preHandle(this.request, null, new Object())).isTrue();

		verify(this.rateLimitService, never()).consume(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	@DisplayName("Should ignore non-auth routes")
	void shouldSkipUnmatchedRoute() {
		when(this.properties.isEnabled()).thenReturn(true);
		when(this.request.getServletPath()).thenReturn("/decks");
		when(this.request.getMethod()).thenReturn("GET");

		assertThat(this.interceptor.preHandle(this.request, null, new Object())).isTrue();

		verify(this.rateLimitService, never()).consume(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	@DisplayName("Should fall back to request URI when servlet path is blank")
	void shouldUseRequestUriFallback() {
		when(this.properties.isEnabled()).thenReturn(true);
		when(this.request.getServletPath()).thenReturn(" ");
		when(this.request.getRequestURI()).thenReturn("/auth/login");
		when(this.request.getMethod()).thenReturn("POST");
		when(this.request.getRemoteAddr()).thenReturn("192.0.2.21");
		final AuthRateLimitProperties.Rule rule = new AuthRateLimitProperties.Rule(20,
				java.time.Duration.ofMinutes(5));
		when(this.properties.ruleFor(AuthRateLimitEndpoint.LOGIN)).thenReturn(rule);
		when(this.rateLimitService.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.21", rule))
				.thenReturn(new AuthRateLimitDecision(true, 0, 1));

		assertThat(this.interceptor.preHandle(this.request, null, new Object())).isTrue();
	}

	@Test
	@DisplayName("Should fall back when servlet path is null")
	void shouldUseRequestUriWhenServletPathIsNull() {
		when(this.properties.isEnabled()).thenReturn(true);
		when(this.request.getServletPath()).thenReturn(null);
		when(this.request.getRequestURI()).thenReturn("/auth/login");
		when(this.request.getMethod()).thenReturn("POST");
		when(this.request.getRemoteAddr()).thenReturn("192.0.2.23");
		final AuthRateLimitProperties.Rule rule = new AuthRateLimitProperties.Rule(20,
				java.time.Duration.ofMinutes(5));
		when(this.properties.ruleFor(AuthRateLimitEndpoint.LOGIN)).thenReturn(rule);
		when(this.rateLimitService.consume(AuthRateLimitEndpoint.LOGIN, "192.0.2.23", rule))
				.thenReturn(new AuthRateLimitDecision(true, 0, 1));

		assertThat(this.interceptor.preHandle(this.request, null, new Object())).isTrue();
	}

	@Test
	@DisplayName("Should reject an over-limit request")
	void shouldRejectOverLimit() {
		when(this.properties.isEnabled()).thenReturn(true);
		when(this.request.getServletPath()).thenReturn("/auth/refresh");
		when(this.request.getMethod()).thenReturn("POST");
		when(this.request.getRemoteAddr()).thenReturn("192.0.2.22");
		final AuthRateLimitProperties.Rule rule = new AuthRateLimitProperties.Rule(60,
				java.time.Duration.ofMinutes(5));
		when(this.properties.ruleFor(AuthRateLimitEndpoint.REFRESH)).thenReturn(rule);
		when(this.rateLimitService.consume(AuthRateLimitEndpoint.REFRESH, "192.0.2.22", rule))
				.thenReturn(new AuthRateLimitDecision(false, 42, 61));

		assertThatThrownBy(() -> this.interceptor.preHandle(this.request, null, new Object()))
				.isInstanceOf(RateLimitExceededException.class)
				.hasMessage("Too many authentication requests. Retry later.");
	}
}
