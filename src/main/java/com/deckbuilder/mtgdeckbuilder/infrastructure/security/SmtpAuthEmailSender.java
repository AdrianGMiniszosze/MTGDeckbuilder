package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.application.AuthEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SmtpAuthEmailSender implements AuthEmailSender {

	private final JavaMailSender mailSender;
	private final AuthEmailProperties properties;

	@Override
	public void sendVerification(String email, String rawToken) {
		send(email, "Verify your MTGDeckbuilder email",
				"Verify your email using this link:\n" + actionLink(this.properties.getVerificationPath(), rawToken)
						+ "\n\nIf needed, submit POST /auth/verify-email with JSON body:\n"
						+ "{\"verification_token\":\"" + rawToken + "\"}");
	}

	@Override
	public void sendPasswordReset(String email, String rawToken) {
		send(email, "Reset your MTGDeckbuilder password",
				"Reset your password using this link:\n"
						+ actionLink(this.properties.getPasswordResetPath(), rawToken)
						+ "\n\nIf needed, submit POST /auth/password-reset with JSON body:\n"
						+ "{\"reset_token\":\"" + rawToken + "\",\"new_password\":\"<your new password>\"}");
	}

	private void send(String recipient, String subject, String body) {
		final SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(this.properties.getFrom());
		message.setTo(recipient);
		message.setSubject(subject);
		message.setText(body);
		this.mailSender.send(message);
	}

	private String actionLink(String path, String rawToken) {
		final String base = this.properties.getFrontendBaseUrl().replaceAll("/+$", "");
		final String suffix = path.startsWith("/") ? path : "/" + path;
		return base + suffix + "#token=" + rawToken;
	}
}
