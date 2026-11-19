package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UnauthenticatedException;
import com.deckbuilder.mtgdeckbuilder.model.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Translates the current Spring Security authentication into the application
 * {@link Actor}. JWT claims are authoritative in production; the username
 * fallback keeps tests and other mechanisms working without faking a JWT.
 */
@Component
@RequiredArgsConstructor
public class CurrentActorProvider {

	private final UserRepository userRepository;

	public Actor currentActorOrNull() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			final Long userId = toLong(jwtAuthentication.getToken().getClaim("uid"));
			if (userId != null) {
				final String role = jwtAuthentication.getToken().getClaimAsString("role");
				return new Actor(userId, jwtAuthentication.getToken().getSubject(), "ADMIN".equals(role));
			}
		}
		return this.userRepository.findByUsername(authentication.getName())
				.map(user -> new Actor(user.getId(), user.getUsername(), "ADMIN".equals(user.getRole()))).orElse(null);
	}

	public Actor requireActor() {
		final Actor actor = currentActorOrNull();
		if (actor == null) {
			throw new UnauthenticatedException();
		}
		return actor;
	}

	private static Long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String text && !text.isBlank()) {
			return Long.valueOf(text);
		}
		return null;
	}
}
