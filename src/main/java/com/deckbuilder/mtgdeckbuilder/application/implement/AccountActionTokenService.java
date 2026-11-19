package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.AccountActionTokenRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidAccountActionTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.AccountActionTokenEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthEmailProperties;
import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Creates single-use email-action tokens. Only the SHA-256 digest is persisted;
 * the raw value is returned once to the caller that sends the email.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AccountActionTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int TOKEN_BYTES = 32;

	private final AccountActionTokenRepository tokenRepository;
	private final AuthEmailProperties emailProperties;
	private final Clock clock;

	@Transactional
	public String issue(Long userId, AccountActionPurpose purpose) {
		final LocalDateTime now = LocalDateTime.now(this.clock);
		this.tokenRepository.deleteExpired(now);
		this.tokenRepository.consumeActive(userId, purpose, now);

		final String rawToken = generateToken();
		final AccountActionTokenEntity entity = new AccountActionTokenEntity();
		entity.setUserId(userId);
		entity.setPurpose(purpose);
		entity.setTokenHash(hash(rawToken));
		entity.setCreatedAt(now);
		entity.setExpiresAt(now.plus(lifetime(purpose)));
		this.tokenRepository.save(entity);
		return rawToken;
	}

	@Transactional
	public Long consume(String rawToken, AccountActionPurpose expectedPurpose) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidAccountActionTokenException();
		}
		final AccountActionTokenEntity entity = this.tokenRepository.findByTokenHash(hash(rawToken))
				.orElseThrow(InvalidAccountActionTokenException::new);
		if (entity.getPurpose() != expectedPurpose || entity.getConsumedAt() != null
				|| !entity.getExpiresAt().isAfter(LocalDateTime.now(this.clock))) {
			throw new InvalidAccountActionTokenException();
		}
		entity.setConsumedAt(LocalDateTime.now(this.clock));
		this.tokenRepository.save(entity);
		return entity.getUserId();
	}

	@Scheduled(fixedDelayString = "${app.security.email.token-cleanup-delay:PT1H}")
	@Transactional
	public void removeExpiredTokens() {
		this.tokenRepository.deleteExpired(LocalDateTime.now(this.clock));
	}

	private java.time.Duration lifetime(AccountActionPurpose purpose) {
		return switch (purpose) {
			case EMAIL_VERIFICATION -> this.emailProperties.getVerificationTokenTtl();
			case PASSWORD_RESET -> this.emailProperties.getPasswordResetTokenTtl();
		};
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
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
