package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.RefreshTokenRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidRefreshTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.RefreshTokenEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and rotates opaque refresh tokens. Only the SHA-256 hash is stored;
 * rotation revokes the presented token, and replaying an already revoked token
 * revokes every session of that user.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	@Transactional
	public String issue(Long userId) {
		final LocalDateTime now = LocalDateTime.now();
		this.refreshTokenRepository.deleteExpired(now);

		final String rawToken = generateToken();
		final RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setUserId(userId);
		entity.setTokenHash(hash(rawToken));
		entity.setCreatedAt(now);
		entity.setExpiresAt(now.plus(this.jwtProperties.getRefreshTokenTtl()));
		this.refreshTokenRepository.save(entity);
		return rawToken;
	}

	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public Rotation rotate(String rawToken) {
		final LocalDateTime now = LocalDateTime.now();
		final RefreshTokenEntity entity = this.refreshTokenRepository.findByTokenHash(hash(rawToken))
				.orElseThrow(InvalidRefreshTokenException::new);

		if (entity.getRevokedAt() != null) {
			// Reuse of a rotated token: assume theft and kill every session.
			this.refreshTokenRepository.revokeAllForUser(entity.getUserId(), now);
			throw new InvalidRefreshTokenException();
		}
		if (!entity.getExpiresAt().isAfter(now)) {
			entity.setRevokedAt(now);
			this.refreshTokenRepository.save(entity);
			throw new InvalidRefreshTokenException();
		}

		entity.setRevokedAt(now);
		this.refreshTokenRepository.save(entity);
		return new Rotation(entity.getUserId(), issue(entity.getUserId()));
	}

	@Transactional
	public void revoke(String rawToken) {
		this.refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(entity -> {
			if (entity.getRevokedAt() == null) {
				entity.setRevokedAt(LocalDateTime.now());
				this.refreshTokenRepository.save(entity);
			}
		});
	}

	@Transactional
	public void revokeAllForUser(Long userId) {
		this.refreshTokenRepository.revokeAllForUser(userId, LocalDateTime.now());
	}

	private static String generateToken() {
		final byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String rawToken) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 is mandatory in every Java runtime.
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	public record Rotation(Long userId, String refreshToken) {
	}
}
