package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.LoginRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.EmailRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.PasswordResetConfirmRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.RefreshRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.RegisterRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.TokenResponseDTO;
import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.deckbuilder.apigenerator.openapi.api.model.VerifyEmailRequestDTO;
import com.deckbuilder.mtgdeckbuilder.application.AuthService;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.application.implement.AccountLifecycleService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.UserMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UserNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.CurrentActorProvider;
import com.deckbuilder.mtgdeckbuilder.model.Actor;
import com.deckbuilder.mtgdeckbuilder.model.AuthSession;
import com.deckbuilder.mtgdeckbuilder.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

	@Mock
	private AuthService authService;

	@Mock
	private UserService userService;

	@Mock
	private AccountLifecycleService accountLifecycleService;

	@Mock
	private UserMapper userMapper;

	@Mock
	private CurrentActorProvider currentActorProvider;

	@InjectMocks
	private AuthController authController;

	private static final Actor ACTOR = new Actor(3L, "johndoe", false);

	@Test
	@DisplayName("Should register a user and hash the raw password through the service")
	void shouldRegister() {
		final RegisterRequestDTO request = RegisterRequestDTO.builder().name("John Doe").username("johndoe")
				.email("john@example.com").country("USA").password("raw-secret").build();
		final User created = User.builder().id(3L).username("johndoe").build();
		final UserDTO dto = UserDTO.builder().id(3).name("John Doe").username("johndoe").email("john@example.com")
				.country("USA").build();
		when(this.accountLifecycleService.register(any(User.class))).thenReturn(created);
		when(this.userMapper.toUserDTO(created)).thenReturn(dto);

		final ResponseEntity<UserDTO> response = this.authController.registerUser(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isSameAs(dto);
		final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(this.accountLifecycleService).register(captor.capture());
		assertThat(captor.getValue().getUsername()).isEqualTo("johndoe");
		assertThat(captor.getValue().getHashedPassword()).isEqualTo("raw-secret");
	}

	@Test
	@DisplayName("Should return the token pair on login")
	void shouldLogin() {
		when(this.authService.login("johndoe", "secret")).thenReturn(new AuthSession("access", "refresh", 900L));

		final TokenResponseDTO body = this.authController
			.loginUser(LoginRequestDTO.builder().username("johndoe").password("secret").build())
			.getBody();

		assertThat(body).isNotNull();
		assertThat(body.getAccess_token()).isEqualTo("access");
		assertThat(body.getRefresh_token()).isEqualTo("refresh");
		assertThat(body.getToken_type()).isEqualTo("Bearer");
		assertThat(body.getExpires_in()).isEqualTo(900);
	}

	@Test
	@DisplayName("Should rotate tokens on refresh")
	void shouldRefresh() {
		when(this.authService.refresh("old")).thenReturn(new AuthSession("access", "new", 900L));

		final TokenResponseDTO body = this.authController
			.refreshTokens(RefreshRequestDTO.builder().refresh_token("old").build())
			.getBody();

		assertThat(body).isNotNull();
		assertThat(body.getRefresh_token()).isEqualTo("new");
	}

	@Test
	@DisplayName("Should log out with 204")
	void shouldLogout() {
		final ResponseEntity<Void> response = this.authController
			.logoutUser(RefreshRequestDTO.builder().refresh_token("token").build());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(this.authService).logout("token");
	}

	@Test
	@DisplayName("Should verify email and return 204")
	void shouldVerifyEmail() {
		final ResponseEntity<Void> response = this.authController.verifyEmail(
				VerifyEmailRequestDTO.builder().verification_token("verify-token").build());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(this.accountLifecycleService).verifyEmail("verify-token");
	}

	@Test
	@DisplayName("Should accept verification resend without revealing account state")
	void shouldResendVerification() {
		final ResponseEntity<Void> response = this.authController.resendVerificationEmail(
				EmailRequestDTO.builder().email("jane@example.com").build());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		verify(this.accountLifecycleService).resendVerification("jane@example.com");
	}

	@Test
	@DisplayName("Should accept password reset request without revealing account state")
	void shouldRequestPasswordReset() {
		final ResponseEntity<Void> response = this.authController.requestPasswordReset(
				EmailRequestDTO.builder().email("jane@example.com").build());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		verify(this.accountLifecycleService).requestPasswordReset("jane@example.com");
	}

	@Test
	@DisplayName("Should reset password and return 204")
	void shouldResetPassword() {
		final ResponseEntity<Void> response = this.authController.resetPassword(
				PasswordResetConfirmRequestDTO.builder().reset_token("reset-token")
						.new_password("new-password-123").build());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(this.accountLifecycleService).resetPassword("reset-token", "new-password-123");
	}

	@Test
	@DisplayName("Should return the authenticated user")
	void shouldReturnCurrentUser() {
		final User user = User.builder().id(3L).username("johndoe").build();
		final UserDTO dto = UserDTO.builder().id(3).name("John Doe").username("johndoe").email("john@example.com")
				.country("USA").build();
		when(this.currentActorProvider.requireActor()).thenReturn(ACTOR);
		when(this.userService.findById(3L)).thenReturn(Optional.of(user));
		when(this.userMapper.toUserDTO(user)).thenReturn(dto);

		final ResponseEntity<UserDTO> response = this.authController.getCurrentUser();

		assertThat(response.getBody()).isSameAs(dto);
	}

	@Test
	@DisplayName("Should fail when the authenticated user no longer exists")
	void shouldFailWhenCurrentUserIsGone() {
		when(this.currentActorProvider.requireActor()).thenReturn(ACTOR);
		when(this.userService.findById(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.authController.getCurrentUser()).isInstanceOf(UserNotFoundException.class);
	}
}
