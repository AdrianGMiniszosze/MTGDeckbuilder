package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.AccountActionTokenRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidAccountActionTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.AccountActionTokenEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthEmailProperties;
import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Action Token Service Tests")
class AccountActionTokenServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 2, 1, 12, 0);

	@Mock
	private AccountActionTokenRepository repository;

	private AuthEmailProperties properties;

	private AccountActionTokenService service;

	@BeforeEach
	void setUp() {
		this.properties = new AuthEmailProperties();
		this.service = new AccountActionTokenService(this.repository, this.properties,
				Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
	}

	@Test
	@DisplayName("Should store only a hash and expiry for email verification")
	void shouldIssueVerificationToken() {
		when(this.repository.save(any(AccountActionTokenEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		final String rawToken = this.service.issue(7L, AccountActionPurpose.EMAIL_VERIFICATION);

		final ArgumentCaptor<AccountActionTokenEntity> entity = ArgumentCaptor.forClass(AccountActionTokenEntity.class);
		verify(this.repository).deleteExpired(NOW);
		verify(this.repository).consumeActive(7L, AccountActionPurpose.EMAIL_VERIFICATION, NOW);
		verify(this.repository).save(entity.capture());
		assertThat(rawToken).hasSize(43);
		assertThat(entity.getValue().getUserId()).isEqualTo(7L);
		assertThat(entity.getValue().getPurpose()).isEqualTo(AccountActionPurpose.EMAIL_VERIFICATION);
		assertThat(entity.getValue().getTokenHash()).isEqualTo(sha256(rawToken)).isNotEqualTo(rawToken);
		assertThat(entity.getValue().getCreatedAt()).isEqualTo(NOW);
		assertThat(entity.getValue().getExpiresAt()).isEqualTo(NOW.plusHours(24));
		assertThat(entity.getValue().getConsumedAt()).isNull();
	}

	@Test
	@DisplayName("Should use the shorter password-reset lifetime")
	void shouldIssueResetToken() {
		when(this.repository.save(any(AccountActionTokenEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		this.service.issue(8L, AccountActionPurpose.PASSWORD_RESET);

		final ArgumentCaptor<AccountActionTokenEntity> entity = ArgumentCaptor.forClass(AccountActionTokenEntity.class);
		verify(this.repository).save(entity.capture());
		assertThat(entity.getValue().getPurpose()).isEqualTo(AccountActionPurpose.PASSWORD_RESET);
		assertThat(entity.getValue().getExpiresAt()).isEqualTo(NOW.plusHours(1));
		verify(this.repository).consumeActive(8L, AccountActionPurpose.PASSWORD_RESET, NOW);
	}

	@Test
	@DisplayName("Should consume a valid token exactly once")
	void shouldConsumeValidToken() {
		final String rawToken = "email-token";
		final AccountActionTokenEntity entity = entity(7L, rawToken, AccountActionPurpose.EMAIL_VERIFICATION,
				NOW.plusHours(1), null);
		when(this.repository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(entity));
		when(this.repository.save(entity)).thenReturn(entity);

		assertThat(this.service.consume(rawToken, AccountActionPurpose.EMAIL_VERIFICATION)).isEqualTo(7L);
		assertThat(entity.getConsumedAt()).isEqualTo(NOW);
		verify(this.repository).save(entity);
	}

	@Test
	@DisplayName("Should reject an unknown token")
	void shouldRejectUnknownToken() {
		when(this.repository.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.consume("unknown", AccountActionPurpose.PASSWORD_RESET))
				.isInstanceOf(InvalidAccountActionTokenException.class);
	}

	@Test
	@DisplayName("Should reject blank or missing tokens")
	void shouldRejectBlankTokens() {
		assertThatThrownBy(() -> this.service.consume(null, AccountActionPurpose.PASSWORD_RESET))
				.isInstanceOf(InvalidAccountActionTokenException.class);
		assertThatThrownBy(() -> this.service.consume(" ", AccountActionPurpose.PASSWORD_RESET))
				.isInstanceOf(InvalidAccountActionTokenException.class);
		verify(this.repository, never()).findByTokenHash(any());
	}

	@Test
	@DisplayName("Should reject a token used for a different purpose")
	void shouldRejectWrongPurpose() {
		final String rawToken = "verify-purpose";
		when(this.repository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(entity(7L, rawToken,
				AccountActionPurpose.EMAIL_VERIFICATION, NOW.plusHours(1), null)));

		assertThatThrownBy(() -> this.service.consume(rawToken, AccountActionPurpose.PASSWORD_RESET))
				.isInstanceOf(InvalidAccountActionTokenException.class);
		verify(this.repository, never()).save(any(AccountActionTokenEntity.class));
	}

	@Test
	@DisplayName("Should reject a consumed token")
	void shouldRejectConsumedToken() {
		final String rawToken = "consumed-token";
		when(this.repository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(entity(7L, rawToken,
				AccountActionPurpose.PASSWORD_RESET, NOW.plusHours(1), NOW.minusMinutes(1))));

		assertThatThrownBy(() -> this.service.consume(rawToken, AccountActionPurpose.PASSWORD_RESET))
				.isInstanceOf(InvalidAccountActionTokenException.class);
		verify(this.repository, never()).save(any(AccountActionTokenEntity.class));
	}

	@Test
	@DisplayName("Should reject an expired token")
	void shouldRejectExpiredToken() {
		final String rawToken = "expired-token";
		when(this.repository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(entity(7L, rawToken,
				AccountActionPurpose.PASSWORD_RESET, NOW, null)));

		assertThatThrownBy(() -> this.service.consume(rawToken, AccountActionPurpose.PASSWORD_RESET))
				.isInstanceOf(InvalidAccountActionTokenException.class);
		verify(this.repository, never()).save(any(AccountActionTokenEntity.class));
	}

	@Test
	@DisplayName("Should remove expired tokens periodically")
	void shouldCleanExpiredTokens() {
		when(this.repository.deleteExpired(NOW)).thenReturn(2);

		this.service.removeExpiredTokens();

		verify(this.repository).deleteExpired(NOW);
	}

	@Test
	@DisplayName("Should fail closed if SHA-256 is unavailable")
	void shouldFailIfSha256Unavailable() {
		final Provider[] providers = Security.getProviders();
		try {
			for (Provider provider : providers) {
				Security.removeProvider(provider.getName());
			}
			assertThatThrownBy(() -> this.service.consume("some-token", AccountActionPurpose.PASSWORD_RESET))
					.isInstanceOf(IllegalStateException.class).hasMessage("SHA-256 is unavailable");
		} finally {
			for (int i = 0; i < providers.length; i++) {
				Security.insertProviderAt(providers[i], i + 1);
			}
		}
		verify(this.repository, never()).findByTokenHash(any());
	}

	private static AccountActionTokenEntity entity(Long userId, String rawToken, AccountActionPurpose purpose,
			LocalDateTime expiresAt, LocalDateTime consumedAt) {
		final AccountActionTokenEntity entity = new AccountActionTokenEntity();
		entity.setUserId(userId);
		entity.setPurpose(purpose);
		entity.setTokenHash(sha256(rawToken));
		entity.setCreatedAt(NOW.minusMinutes(1));
		entity.setExpiresAt(expiresAt);
		entity.setConsumedAt(consumedAt);
		return entity;
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
