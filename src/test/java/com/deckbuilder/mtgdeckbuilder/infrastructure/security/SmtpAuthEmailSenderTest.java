package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.security.SmtpAuthEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("SMTP Auth Email Sender Tests")
class SmtpAuthEmailSenderTest {

	@Test
	@DisplayName("Should send a verification email with a frontend deep link")
	void shouldSendVerification() {
		final JavaMailSender mailSender = mock(JavaMailSender.class);
		final AuthEmailProperties properties = new AuthEmailProperties();
		properties.setFrom("security@example.com");
		properties.setFrontendBaseUrl("https://app.example.com/");
		properties.setVerificationPath("verify-email");
		final SmtpAuthEmailSender sender = new SmtpAuthEmailSender(mailSender, properties);

		sender.sendVerification("jane@example.com", "raw-token");

		final SimpleMailMessage message = capture(mailSender);
		assertThat(message.getFrom()).isEqualTo("security@example.com");
		assertThat(message.getTo()).containsExactly("jane@example.com");
		assertThat(message.getSubject()).contains("Verify");
		assertThat(message.getText()).contains("https://app.example.com/verify-email#token=raw-token")
				.contains("POST /auth/verify-email").contains("\"verification_token\":\"raw-token\"");
	}

	@Test
	@DisplayName("Should send a password reset email")
	void shouldSendPasswordReset() {
		final JavaMailSender mailSender = mock(JavaMailSender.class);
		final AuthEmailProperties properties = new AuthEmailProperties();
		properties.setFrontendBaseUrl("https://app.example.com");
		properties.setPasswordResetPath("/reset-password");
		final SmtpAuthEmailSender sender = new SmtpAuthEmailSender(mailSender, properties);

		sender.sendPasswordReset("john@example.com", "reset-token");

		final SimpleMailMessage message = capture(mailSender);
		assertThat(message.getTo()).containsExactly("john@example.com");
		assertThat(message.getSubject()).contains("Reset");
		assertThat(message.getText()).contains("https://app.example.com/reset-password#token=reset-token")
				.contains("POST /auth/password-reset").contains("\"reset_token\":\"reset-token\"")
				.contains("\"new_password\":\"<your new password>\"");
	}

	private static SimpleMailMessage capture(JavaMailSender sender) {
		final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(captor.capture());
		return captor.getValue();
	}
}
