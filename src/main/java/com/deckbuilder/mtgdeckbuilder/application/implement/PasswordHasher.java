package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidUserDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Turns a raw password into the value stored in {@code users.hashed_password}
 * and verifies it. BCrypt caps the input at 72 bytes, so longer values are
 * rejected instead of silently truncated.
 */
@Component
@RequiredArgsConstructor
public class PasswordHasher {

	static final int MAX_PASSWORD_BYTES = 72;
	static final int MIN_PASSWORD_LENGTH = 8;

	private final PasswordEncoder passwordEncoder;

	public String hash(String rawPassword) {
		if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
			throw new InvalidUserDataException("password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}
		if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new InvalidUserDataException(
					"hashed_password must be at most " + MAX_PASSWORD_BYTES + " bytes (UTF-8)");
		}
		return this.passwordEncoder.encode(rawPassword);
	}

	public boolean matches(String rawPassword, String hashedPassword) {
		return this.passwordEncoder.matches(rawPassword, hashedPassword);
	}
}
