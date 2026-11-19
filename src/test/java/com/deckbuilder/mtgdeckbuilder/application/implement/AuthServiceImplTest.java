package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.AccountDisabledException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.EmailNotVerifiedException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidCredentialsException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidRefreshTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.JwtService;
import com.deckbuilder.mtgdeckbuilder.model.AuthSession;
import com.deckbuilder.mtgdeckbuilder.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceImplTest {

	@Mock
	private UserService userService;

	@Mock
	private PasswordHasher passwordHasher;

	@Mock
	private JwtService jwtService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private AuthServiceImpl authService;

	private User activeUser;

	@BeforeEach
	void setUp() {
		this.activeUser = User.builder().id(3L).username("johndoe").email("john@example.com")
				.hashedPassword("stored-hash").role("USER").isActive(true)
				.emailVerifiedAt(java.time.LocalDateTime.now()).build();
	}

	@Test
	@DisplayName("Should log in with a valid password")
	void shouldLogin() {
		when(this.userService.findByUsernameOrEmail("johndoe")).thenReturn(Optional.of(this.activeUser));
		when(this.passwordHasher.matches("secret", "stored-hash")).thenReturn(true);
		when(this.userService.recordLogin(3L)).thenReturn(this.activeUser);
		when(this.jwtService.issueAccessToken(this.activeUser)).thenReturn("access");
		when(this.refreshTokenService.issue(3L)).thenReturn("refresh");
		when(this.jwtService.accessTokenSeconds()).thenReturn(900L);

		final AuthSession session = this.authService.login("johndoe", "secret");

		assertThat(session).isEqualTo(new AuthSession("access", "refresh", 900L));
		verify(this.userService).recordLogin(3L);
	}

	@Test
	@DisplayName("Should fail generically when the user does not exist")
	void shouldRejectUnknownUser() {
		when(this.userService.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.authService.login("ghost", "secret"))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(this.passwordHasher).matches(eq("secret"), anyString());
		verify(this.userService, never()).recordLogin(any());
	}

	@Test
	@DisplayName("Should reject a wrong password")
	void shouldRejectWrongPassword() {
		when(this.userService.findByUsernameOrEmail("johndoe")).thenReturn(Optional.of(this.activeUser));
		when(this.passwordHasher.matches("wrong", "stored-hash")).thenReturn(false);

		assertThatThrownBy(() -> this.authService.login("johndoe", "wrong"))
			.isInstanceOf(InvalidCredentialsException.class);
		verify(this.userService, never()).recordLogin(any());
	}

	@Test
	@DisplayName("Should reject a disabled account")
	void shouldRejectDisabledAccount() {
		final User disabled = this.activeUser.toBuilder().isActive(false).build();
		when(this.userService.findByUsernameOrEmail("johndoe")).thenReturn(Optional.of(disabled));
		when(this.passwordHasher.matches("secret", "stored-hash")).thenReturn(true);

		assertThatThrownBy(() -> this.authService.login("johndoe", "secret"))
			.isInstanceOf(AccountDisabledException.class);
		verify(this.userService, never()).recordLogin(any());
	}

	@Test
	@DisplayName("Should reject login until email is verified")
	void shouldRejectUnverifiedEmail() {
		final User unverified = this.activeUser.toBuilder().emailVerifiedAt(null).build();
		when(this.userService.findByUsernameOrEmail("johndoe")).thenReturn(Optional.of(unverified));
		when(this.passwordHasher.matches("secret", "stored-hash")).thenReturn(true);

		assertThatThrownBy(() -> this.authService.login("johndoe", "secret"))
				.isInstanceOf(EmailNotVerifiedException.class);
		verify(this.userService, never()).recordLogin(any());
	}

	@Test
	@DisplayName("Should rotate and return a new session")
	void shouldRefresh() {
		when(this.refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(3L, "new"));
		when(this.userService.findById(3L)).thenReturn(Optional.of(this.activeUser));
		when(this.jwtService.issueAccessToken(this.activeUser)).thenReturn("access");
		when(this.jwtService.accessTokenSeconds()).thenReturn(900L);

		final AuthSession session = this.authService.refresh("old");

		assertThat(session).isEqualTo(new AuthSession("access", "new", 900L));
	}

	@Test
	@DisplayName("Should reject a refresh token whose user is gone")
	void shouldRejectRefreshWithoutUser() {
		when(this.refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(3L, "new"));
		when(this.userService.findById(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.authService.refresh("old"))
			.isInstanceOf(InvalidRefreshTokenException.class);
		verify(this.refreshTokenService).revokeAllForUser(3L);
	}

	@Test
	@DisplayName("Should reject a refresh token for a disabled account")
	void shouldRejectRefreshForDisabledAccount() {
		final User disabled = this.activeUser.toBuilder().isActive(false).build();
		when(this.refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(3L, "new"));
		when(this.userService.findById(3L)).thenReturn(Optional.of(disabled));

		assertThatThrownBy(() -> this.authService.refresh("old")).isInstanceOf(AccountDisabledException.class);
		verify(this.refreshTokenService).revokeAllForUser(3L);
	}

	@Test
	@DisplayName("Should revoke refresh sessions until email is verified")
	void shouldRejectRefreshForUnverifiedEmail() {
		final User unverified = this.activeUser.toBuilder().emailVerifiedAt(null).build();
		when(this.refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(3L, "new"));
		when(this.userService.findById(3L)).thenReturn(Optional.of(unverified));

		assertThatThrownBy(() -> this.authService.refresh("old")).isInstanceOf(EmailNotVerifiedException.class);
		verify(this.refreshTokenService).revokeAllForUser(3L);
	}

	@Test
	@DisplayName("Should revoke the refresh token on logout")
	void shouldLogout() {
		this.authService.logout("token");

		verify(this.refreshTokenService).revoke("token");
	}
}
