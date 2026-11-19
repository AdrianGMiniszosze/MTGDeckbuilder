package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.AuthEmailSender;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidAccountActionTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthEmailDeliveryMetrics;
import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import com.deckbuilder.mtgdeckbuilder.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Lifecycle Service Tests")
class AccountLifecycleServiceTest {

	@Mock
	private UserService userService;

	@Mock
	private AccountActionTokenService actionTokenService;

	@Mock
	private AuthEmailSender emailSender;

	@Mock
	private AuthEmailDeliveryMetrics emailDeliveryMetrics;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private AccountLifecycleService service;

	@Test
	@DisplayName("Should create an unverified user and send verification")
	void shouldRegisterUnverifiedUser() {
		final User submitted = User.builder().name("Jane").email("jane@example.com").build();
		final User created = user(4L, "jane@example.com", false, true);
		when(this.userService.createUnverified(submitted)).thenReturn(created);
		when(this.actionTokenService.issue(4L, AccountActionPurpose.EMAIL_VERIFICATION)).thenReturn("verify-token");

		assertThat(this.service.register(submitted)).isSameAs(created);

		verify(this.emailSender).sendVerification("jane@example.com", "verify-token");
	}

	@Test
	@DisplayName("Should consume verification token and mark user verified")
	void shouldVerifyEmail() {
		when(this.actionTokenService.consume("verify-token", AccountActionPurpose.EMAIL_VERIFICATION)).thenReturn(4L);

		this.service.verifyEmail("verify-token");

		verify(this.userService).verifyEmail(4L);
	}

	@Test
	@DisplayName("Should not reveal whether a verification address exists")
	void shouldResendVerificationGenerically() {
		when(this.userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
		when(this.userService.findByEmail("verified@example.com"))
				.thenReturn(Optional.of(user(5L, "verified@example.com", true, true)));
		when(this.userService.findByEmail("unverified@example.com"))
				.thenReturn(Optional.of(user(6L, "unverified@example.com", false, true)));
		when(this.actionTokenService.issue(6L, AccountActionPurpose.EMAIL_VERIFICATION)).thenReturn("resend-token");

		this.service.resendVerification("unknown@example.com");
		this.service.resendVerification("verified@example.com");
		this.service.resendVerification("unverified@example.com");

		verify(this.emailSender).sendVerification("unverified@example.com", "resend-token");
		verify(this.emailSender, never()).sendVerification(eq("verified@example.com"), any());
		verify(this.emailSender, never()).sendVerification(eq("unknown@example.com"), any());
	}

	@Test
	@DisplayName("Should not resend verification to a disabled account")
	void shouldSkipDisabledAccountResend() {
		when(this.userService.findByEmail("disabled@example.com"))
				.thenReturn(Optional.of(user(7L, "disabled@example.com", false, false)));

		this.service.resendVerification("disabled@example.com");

		verify(this.actionTokenService, never()).issue(any(), any());
		verify(this.emailSender, never()).sendVerification(any(), any());
	}

	@Test
	@DisplayName("Should return a generic password reset request for unknown addresses")
	void shouldHandleUnknownPasswordResetAddress() {
		when(this.userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

		this.service.requestPasswordReset("unknown@example.com");

		verify(this.actionTokenService, never()).issue(any(), any());
		verify(this.emailSender, never()).sendPasswordReset(any(), any());
	}

	@Test
	@DisplayName("Should send password reset for an active account")
	void shouldSendPasswordReset() {
		when(this.userService.findByEmail("jane@example.com"))
				.thenReturn(Optional.of(user(8L, "jane@example.com", false, true)));
		when(this.actionTokenService.issue(8L, AccountActionPurpose.PASSWORD_RESET)).thenReturn("reset-token");

		this.service.requestPasswordReset("jane@example.com");

		verify(this.emailSender).sendPasswordReset("jane@example.com", "reset-token");
	}

	@Test
	@DisplayName("Should skip password reset for a disabled account")
	void shouldSkipDisabledPasswordReset() {
		when(this.userService.findByEmail("disabled@example.com"))
				.thenReturn(Optional.of(user(9L, "disabled@example.com", true, false)));

		this.service.requestPasswordReset("disabled@example.com");

		verify(this.actionTokenService, never()).issue(any(), any());
		verify(this.emailSender, never()).sendPasswordReset(any(), any());
	}

	@Test
	@DisplayName("Should reset password, verify its email and revoke refresh sessions")
	void shouldResetPassword() {
		when(this.actionTokenService.consume("reset-token", AccountActionPurpose.PASSWORD_RESET)).thenReturn(10L);

		this.service.resetPassword("reset-token", "new-secret-123");

		verify(this.userService).resetPassword(10L, "new-secret-123");
		verify(this.refreshTokenService).revokeAllForUser(10L);
	}

	@Test
	@DisplayName("Should not update the account when reset token is invalid")
	void shouldRejectInvalidPasswordReset() {
		when(this.actionTokenService.consume("bad-token", AccountActionPurpose.PASSWORD_RESET))
				.thenThrow(new InvalidAccountActionTokenException());

		assertThatThrownBy(() -> this.service.resetPassword("bad-token", "new-secret-123"))
				.isInstanceOf(InvalidAccountActionTokenException.class);
		verify(this.userService, never()).resetPassword(any(), any());
		verify(this.refreshTokenService, never()).revokeAllForUser(any());
	}

	@Test
	@DisplayName("Should swallow verification SMTP errors and count them")
	void shouldHandleVerificationMailFailure() {
		final User submitted = User.builder().name("Jane").email("jane@example.com").build();
		final User created = user(11L, "jane@example.com", false, true);
		when(this.userService.createUnverified(submitted)).thenReturn(created);
		when(this.actionTokenService.issue(11L, AccountActionPurpose.EMAIL_VERIFICATION)).thenReturn("verify-token");
		org.mockito.Mockito.doThrow(new MailSendException("smtp down"))
				.when(this.emailSender).sendVerification("jane@example.com", "verify-token");

		assertThat(this.service.register(submitted)).isSameAs(created);

		verify(this.emailDeliveryMetrics).deliveryFailed(AccountActionPurpose.EMAIL_VERIFICATION);
	}

	@Test
	@DisplayName("Should swallow password reset SMTP errors and count them")
	void shouldHandleResetMailFailure() {
		when(this.userService.findByEmail("jane@example.com"))
				.thenReturn(Optional.of(user(12L, "jane@example.com", true, true)));
		when(this.actionTokenService.issue(12L, AccountActionPurpose.PASSWORD_RESET)).thenReturn("reset-token");
		org.mockito.Mockito.doThrow(new MailSendException("smtp down"))
				.when(this.emailSender).sendPasswordReset("jane@example.com", "reset-token");

		this.service.requestPasswordReset("jane@example.com");

		verify(this.emailDeliveryMetrics).deliveryFailed(AccountActionPurpose.PASSWORD_RESET);
	}

	private static User user(Long id, String email, boolean verified, boolean active) {
		return User.builder().id(id).name("Test").username("testuser").email(email).role("USER").isActive(active)
				.emailVerifiedAt(verified ? java.time.LocalDateTime.now() : null).build();
	}
}
