package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CORS Properties Tests")
class CorsPropertiesTest {

	@Test
	@DisplayName("Should allow empty lists and exact local or HTTPS origins")
	void shouldAllowExactOrigins() {
		final CorsProperties properties = new CorsProperties();
		assertThatCode(properties::validate).doesNotThrowAnyException();

		properties.setAllowedOrigins(List.of("http://localhost:3000", "https://app.example.com", "https://root.example"));
		assertThatCode(properties::validate).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should reject a missing list, blank origin or wildcard")
	void shouldRejectMissingOrWildcardOrigins() {
		final CorsProperties properties = new CorsProperties();
		properties.setAllowedOrigins(null);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("CORS allowed origins must be a list");

		properties.setAllowedOrigins(new ArrayList<>(List.of("https://valid.example")));
		properties.getAllowedOrigins().add(null);
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("CORS origins must be explicit HTTP(S) origins");
		properties.setAllowedOrigins(List.of(" "));
		assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
		properties.setAllowedOrigins(List.of("*"));
		assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Should reject malformed or non-origin URIs")
	void shouldRejectInvalidUris() {
		final CorsProperties properties = new CorsProperties();
		properties.setAllowedOrigins(List.of("https://["));
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("CORS origin is not a valid URI");

		for (String invalid : List.of("mailto:admin@example.com", "https://user@example.com",
				"https://example.com?x=1", "https://example.com#fragment", "https://example.com/path",
				"https:///")) {
			properties.setAllowedOrigins(List.of(invalid));
			assertThatThrownBy(properties::validate)
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("CORS entries must be exact HTTP(S) origins without paths");
		}
	}
}
