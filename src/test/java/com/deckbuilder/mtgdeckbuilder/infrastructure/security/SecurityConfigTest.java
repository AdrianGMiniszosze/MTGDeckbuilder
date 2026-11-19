package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Security Config Tests")
class SecurityConfigTest {

	@Test
	@DisplayName("Should map the role claim to a Spring authority")
	void shouldConvertRoleClaim() {
		final SecurityConfig config = config();
		final Jwt token = Jwt.withTokenValue("token").header("alg", "HS256").subject("admin")
				.claim("role", "ADMIN").build();

		assertThat(config.jwtAuthenticationConverter().convert(token).getAuthorities())
				.containsExactly(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	@Test
	@DisplayName("Should return no authorities when role claim is absent")
	void shouldHandleMissingRoleClaim() {
		final JwtAuthenticationConverter converter = config().jwtAuthenticationConverter();
		final Jwt token = Jwt.withTokenValue("token").header("alg", "HS256").subject("johndoe").build();

		assertThat(converter.convert(token).getAuthorities()).isEmpty();
	}

	private static SecurityConfig config() {
		return new SecurityConfig(mock(JwtService.class),
				new RestAuthenticationEntryPoint(new ObjectMapper()),
				new RestAccessDeniedHandler(new ObjectMapper()));
	}
}
