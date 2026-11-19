package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.PasswordHasher;
import com.deckbuilder.mtgdeckbuilder.infrastructure.config.PasswordEncodingConfig;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidUserDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Password Hasher Tests")
class PasswordHasherTest {

	private PasswordHasher passwordHasher;

	@BeforeEach
	void setUp() {
		this.passwordHasher = new PasswordHasher(new PasswordEncodingConfig().passwordEncoder());
	}

	@Test
	@DisplayName("Should store a BCrypt hash instead of the raw password")
	void shouldHashPassword() {
		// When
		final String hashed = this.passwordHasher.hash("plain-secret-123");

		// Then
		assertThat(hashed).startsWith("$2").isNotEqualTo("plain-secret-123");
		assertThat(this.passwordHasher.matches("plain-secret-123", hashed)).isTrue();
		assertThat(this.passwordHasher.matches("wrong-secret", hashed)).isFalse();
	}

	@Test
	@DisplayName("Should salt every hash")
	void shouldSaltEveryHash() {
		// When
		final String first = this.passwordHasher.hash("same-password");
		final String second = this.passwordHasher.hash("same-password");

		// Then
		assertThat(first).isNotEqualTo(second);
		assertThat(this.passwordHasher.matches("same-password", first)).isTrue();
		assertThat(this.passwordHasher.matches("same-password", second)).isTrue();
	}

	@Test
	@DisplayName("Should accept 72 bytes")
	void shouldAcceptMaximumLength() {
		// Given
		final String raw = "a".repeat(72);

		// When
		final String hashed = this.passwordHasher.hash(raw);

		// Then
		assertThat(this.passwordHasher.matches(raw, hashed)).isTrue();
	}

	@Test
	@DisplayName("Should reject passwords longer than 72 bytes")
	void shouldRejectTooLongPassword() {
		// Given
		final String raw = "a".repeat(73);

		// When/Then
		assertThatThrownBy(() -> this.passwordHasher.hash(raw)).isInstanceOf(InvalidUserDataException.class)
			.hasMessageContaining("at most 72 bytes");
	}

	@Test
	@DisplayName("Should reject passwords shorter than 8 characters")
	void shouldRejectTooShortPassword() {
		assertThatThrownBy(() -> this.passwordHasher.hash("short"))
			.isInstanceOf(InvalidUserDataException.class)
			.hasMessageContaining("at least 8 characters");
	}

	@Test
	@DisplayName("Should reject a null password")
	void shouldRejectNullPassword() {
		assertThatThrownBy(() -> this.passwordHasher.hash(null))
			.isInstanceOf(InvalidUserDataException.class);
	}

	@Test
	@DisplayName("Should count UTF-8 bytes, not characters")
	void shouldCountBytesForMultibyteCharacters() {
		// Given - 37 two-byte characters are 74 bytes
		final String raw = "ñ".repeat(37);

		// When/Then
		assertThatThrownBy(() -> this.passwordHasher.hash(raw)).isInstanceOf(InvalidUserDataException.class);
	}
}
