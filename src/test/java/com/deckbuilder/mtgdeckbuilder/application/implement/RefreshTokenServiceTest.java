package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.RefreshTokenRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidRefreshTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.RefreshTokenEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Refresh Token Service Tests")
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private JwtProperties jwtProperties;

	private RefreshTokenService service;

	@BeforeEach
	void setUp() {
		this.jwtProperties = new JwtProperties();
		this.jwtProperties.setSecret("unit-test-secret-0123456789abcdef0123456789abcdef");
		this.jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));
		this.service = new RefreshTokenService(this.refreshTokenRepository, this.jwtProperties);
	}

	@Test
	@DisplayName("Should store only a hash of the issued token")
	void shouldIssueHashedToken() {
		when(this.refreshTokenRepository.save(any(RefreshTokenEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final String token = this.service.issue(5L);

		final ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
		verify(this.refreshTokenRepository).save(captor.capture());
		final RefreshTokenEntity saved = captor.getValue();

		assertThat(token).isNotBlank();
		assertThat(saved.getUserId()).isEqualTo(5L);
		assertThat(saved.getTokenHash()).isEqualTo(sha256(token)).isNotEqualTo(token);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
		assertThat(saved.getRevokedAt()).isNull();
		verify(this.refreshTokenRepository).deleteExpired(any(LocalDateTime.class));
	}

	@Test
	@DisplayName("Should reject an unknown token")
	void shouldRejectUnknownToken() {
		when(this.refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.rotate("unknown")).isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("Should rotate a valid token and revoke the presented one")
	void shouldRotateValidToken() {
		final String raw = "valid-token";
		final RefreshTokenEntity entity = token(5L, raw, LocalDateTime.now().plusDays(1));
		when(this.refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(entity));
		when(this.refreshTokenRepository.save(any(RefreshTokenEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final RefreshTokenService.Rotation rotation = this.service.rotate(raw);

		assertThat(rotation.userId()).isEqualTo(5L);
		assertThat(rotation.refreshToken()).isNotBlank().isNotEqualTo(raw);
		assertThat(entity.getRevokedAt()).isNotNull();
		verify(this.refreshTokenRepository).save(entity);
	}

	@Test
	@DisplayName("Should revoke every session when a rotated token is replayed")
	void shouldRevokeFamilyOnReplay() {
		final String raw = "replayed-token";
		final RefreshTokenEntity entity = token(5L, raw, LocalDateTime.now().plusDays(1));
		entity.setRevokedAt(LocalDateTime.now().minusMinutes(1));
		when(this.refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(entity));

		assertThatThrownBy(() -> this.service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
		verify(this.refreshTokenRepository).revokeAllForUser(eq(5L), any(LocalDateTime.class));
		verify(this.refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
	}

	@Test
	@DisplayName("Should reject an expired token and revoke it")
	void shouldRejectExpiredToken() {
		final String raw = "expired-token";
		final RefreshTokenEntity entity = token(5L, raw, LocalDateTime.now().minusSeconds(1));
		when(this.refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(entity));
		when(this.refreshTokenRepository.save(any(RefreshTokenEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		assertThatThrownBy(() -> this.service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
		assertThat(entity.getRevokedAt()).isNotNull();
		verify(this.refreshTokenRepository, never()).revokeAllForUser(anyLong(), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("Should revoke a known token")
	void shouldRevokeToken() {
		final String raw = "logout-token";
		final RefreshTokenEntity entity = token(5L, raw, LocalDateTime.now().plusDays(1));
		when(this.refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(entity));
		when(this.refreshTokenRepository.save(any(RefreshTokenEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		this.service.revoke(raw);

		assertThat(entity.getRevokedAt()).isNotNull();
		verify(this.refreshTokenRepository).save(entity);
	}

	@Test
	@DisplayName("Should not rewrite an already revoked token")
	void shouldIgnoreAlreadyRevokedToken() {
		final String raw = "already-revoked";
		final RefreshTokenEntity entity = token(5L, raw, LocalDateTime.now().plusDays(1));
		entity.setRevokedAt(LocalDateTime.now().minusMinutes(1));
		when(this.refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(entity));

		this.service.revoke(raw);

		verify(this.refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
	}

	@Test
	@DisplayName("Should ignore an unknown token on logout")
	void shouldIgnoreUnknownOnLogout() {
		when(this.refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		this.service.revoke("ghost");

		verify(this.refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
	}

	@Test
	@DisplayName("Should revoke all sessions for a user")
	void shouldRevokeAllSessionsForUser() {
		this.service.revokeAllForUser(5L);

		verify(this.refreshTokenRepository).revokeAllForUser(eq(5L), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("Should fail closed if SHA-256 is unavailable")
	void shouldFailWhenSha256IsUnavailable() throws Exception {
		final Provider[] providers = Security.getProviders();
		final Method hash = RefreshTokenService.class.getDeclaredMethod("hash", String.class);
		hash.setAccessible(true);
		try {
			for (Provider provider : providers) {
				Security.removeProvider(provider.getName());
			}

			assertThatThrownBy(() -> hash.invoke(null, "token"))
				.hasRootCauseInstanceOf(java.security.NoSuchAlgorithmException.class);
		} finally {
			for (int i = 0; i < providers.length; i++) {
				Security.insertProviderAt(providers[i], i + 1);
			}
		}
	}

	private static RefreshTokenEntity token(Long userId, String raw, LocalDateTime expiresAt) {
		final RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setUserId(userId);
		entity.setTokenHash(sha256(raw));
		entity.setCreatedAt(LocalDateTime.now().minusMinutes(5));
		entity.setExpiresAt(expiresAt);
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
