package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthRateLimitInterceptor implements HandlerInterceptor {

	private final AuthRateLimitService rateLimitService;
	private final AuthRateLimitProperties properties;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!this.properties.isEnabled()) {
			return true;
		}
		final String path = request.getServletPath() == null || request.getServletPath().isBlank()
				? request.getRequestURI()
				: request.getServletPath();
		final AuthRateLimitEndpoint endpoint = AuthRateLimitEndpoint.resolve(request.getMethod(), path).orElse(null);
		if (endpoint == null) {
			return true;
		}

		final AuthRateLimitDecision decision = this.rateLimitService.consume(endpoint, request.getRemoteAddr(),
				this.properties.ruleFor(endpoint));
		if (!decision.allowed()) {
			throw new RateLimitExceededException(decision.retryAfterSeconds());
		}
		return true;
	}
}
