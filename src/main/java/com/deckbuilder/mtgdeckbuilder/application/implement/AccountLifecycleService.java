package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.AuthEmailSender;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidAccountActionTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthEmailDeliveryMetrics;
import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import com.deckbuilder.mtgdeckbuilder.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email verification and password recovery. Requests that could reveal whether
 * an address exists always have the same empty 202 response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AccountLifecycleService {

	private final UserService userService;
	private final AccountActionTokenService actionTokenService;
	private final AuthEmailSender emailSender;
	private final AuthEmailDeliveryMetrics emailDeliveryMetrics;
	private final RefreshTokenService refreshTokenService;

	public User register(User user) {
		final User created = this.userService.createUnverified(user);
		sendVerification(created);
		return created;
	}

	@Transactional
	public void verifyEmail(String rawToken) {
		final Long userId = this.actionTokenService.consume(rawToken, AccountActionPurpose.EMAIL_VERIFICATION);
		this.userService.verifyEmail(userId);
	}

	public void resendVerification(String email) {
		this.userService.findByEmail(email)
				.filter(user -> user.getEmailVerifiedAt() == null && Boolean.TRUE.equals(user.getIsActive()))
				.ifPresent(this::sendVerification);
	}

	public void requestPasswordReset(String email) {
		this.userService.findByEmail(email)
				.filter(user -> Boolean.TRUE.equals(user.getIsActive()))
				.ifPresent(this::sendPasswordReset);
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		final Long userId = this.actionTokenService.consume(rawToken, AccountActionPurpose.PASSWORD_RESET);
		this.userService.resetPassword(userId, newPassword);
		this.refreshTokenService.revokeAllForUser(userId);
	}

	private void sendVerification(User user) {
		final String token = this.actionTokenService.issue(user.getId(), AccountActionPurpose.EMAIL_VERIFICATION);
		sendSafely(user.getEmail(), token, AccountActionPurpose.EMAIL_VERIFICATION);
	}

	private void sendPasswordReset(User user) {
		final String token = this.actionTokenService.issue(user.getId(), AccountActionPurpose.PASSWORD_RESET);
		sendSafely(user.getEmail(), token, AccountActionPurpose.PASSWORD_RESET);
	}

	private void sendSafely(String email, String token, AccountActionPurpose purpose) {
		try {
			if (purpose == AccountActionPurpose.EMAIL_VERIFICATION) {
				this.emailSender.sendVerification(email, token);
			} else {
				this.emailSender.sendPasswordReset(email, token);
			}
		} catch (MailException exception) {
			// Do not log email addresses or bearer-like action tokens. The user can resend.
			log.warn("Authentication email delivery failed; purpose={}", purpose);
			this.emailDeliveryMetrics.deliveryFailed(purpose);
		}
	}
}
