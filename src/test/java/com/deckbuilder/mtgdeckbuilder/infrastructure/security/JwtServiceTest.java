package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWT Service Tests")
class JwtServiceTest {

	private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef";

	private static JwtProperties properties() {
		final JwtProperties properties = new JwtProperties();
		properties.setSecret(SECRET);
		properties.setIssuer("mtgdeckbuilder-test");
		properties.setAccessTokenTtl(Duration.ofMinutes(15));
		return properties;
	}

	private static User user(String role) {
		return User.builder().id(7L).username("johndoe").role(role).build();
	}

	@Test
	@DisplayName("Should issue a decodable token with the expected claims")
	void shouldIssueTokenWithClaims() {
		final JwtService service = new JwtService(properties());

		final String token = service.issueAccessToken(user("ADMIN"));
		final Jwt jwt = service.decoder().decode(token);

		assertThat(jwt.getSubject()).isEqualTo("johndoe");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("mtgdeckbuilder-test");
		assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
		assertThat(jwt.<Long>getClaim("uid")).isEqualTo(7L);
		assertThat(jwt.getExpiresAt()).isNotNull();
	}

	@Test
	@DisplayName("Should default a missing role to USER")
	void shouldDefaultRoleToUser() {
		final JwtService service = new JwtService(properties());

		final Jwt jwt = service.decoder().decode(service.issueAccessToken(user(null)));

		assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
	}

	@Test
	@DisplayName("Should expose the access token lifetime in seconds")
	void shouldExposeLifetime() {
		assertThat(new JwtService(properties()).accessTokenSeconds()).isEqualTo(900);
	}

	@Test
	@DisplayName("Should reject a missing secret")
	void shouldRejectMissingSecret() {
		final JwtProperties properties = properties();
		properties.setSecret(null);
		final JwtService service = new JwtService(properties);

		assertThatThrownBy(service::decoder).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("app.security.jwt.secret");
	}

	@Test
	@DisplayName("Should reject a secret shorter than 32 bytes")
	void shouldRejectShortSecret() {
		final JwtProperties properties = properties();
		properties.setSecret("too-short");
		final JwtService service = new JwtService(properties);

		assertThatThrownBy(() -> service.issueAccessToken(user("USER"))).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes");
	}
}
