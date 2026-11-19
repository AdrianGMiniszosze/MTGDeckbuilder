package com.deckbuilder.mtgdeckbuilder.infrastructure.config;

import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthEmailProperties;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthRateLimitProperties;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.CorsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.net.URI;

/**
 * Fail-fast checks for configuration mistakes that would make production
 * authentication or email links unsafe or unusable.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionConfigurationValidator {

	private static final Set<String> LOCAL_SMTP_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "mailpit");

	private final Environment environment;
	private final AuthEmailProperties emailProperties;
	private final AuthRateLimitProperties rateLimitProperties;
	private final CorsProperties corsProperties;

	@PostConstruct
	void validate() {
		requireRemoteDatabase(this.environment.getProperty("spring.datasource.url"));
		requireHttpsFrontend(this.emailProperties.getFrontendBaseUrl());
		requireHttpsCorsOrigins(this.corsProperties.getAllowedOrigins());
		requireFrontendOriginAllowed();
		requireRemoteSmtp(this.environment.getProperty("spring.mail.host"));
		requireSecureSmtpTransport();
		requireCredentialsWhenSmtpAuthEnabled();
		requireSharedRateLimits();
		requireFreshFlywayInstallation();
	}

	private static void requireRemoteDatabase(String url) {
		if (url == null || url.isBlank() || isLoopback(url)) {
			throw new IllegalStateException("Production requires a configured remote PostgreSQL URL");
		}
	}

	private static void requireHttpsFrontend(String frontendUrl) {
		if (frontendUrl == null || !frontendUrl.startsWith("https://")) {
			throw new IllegalStateException("Production APP_PUBLIC_URL must use HTTPS");
		}
	}

	private static void requireHttpsCorsOrigins(Iterable<String> origins) {
		if (origins == null || !origins.iterator().hasNext()) {
			throw new IllegalStateException("Production requires at least one explicit CORS origin");
		}
		for (String origin : origins) {
			if (origin == null || !origin.startsWith("https://")) {
				throw new IllegalStateException("Production CORS origins must be explicit HTTPS origins");
			}
		}
	}

	private void requireFrontendOriginAllowed() {
		final URI frontend = URI.create(this.emailProperties.getFrontendBaseUrl());
		final String frontendOrigin = frontend.getScheme() + "://" + frontend.getRawAuthority();
		if (!this.corsProperties.getAllowedOrigins().contains(frontendOrigin)) {
			throw new IllegalStateException("APP_PUBLIC_URL origin must be included in CORS_ALLOWED_ORIGINS");
		}
	}

	private static void requireRemoteSmtp(String host) {
		if (host == null || host.isBlank()
				|| LOCAL_SMTP_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
			throw new IllegalStateException("Production MAIL_HOST must be a remote SMTP provider");
		}
	}

	private void requireSecureSmtpTransport() {
		final boolean startTls = this.environment.getProperty(
				"spring.mail.properties.mail.smtp.starttls.enable", Boolean.class, false);
		final boolean ssl = this.environment.getProperty(
				"spring.mail.properties.mail.smtp.ssl.enable", Boolean.class, false);
		if (!startTls && !ssl) {
			throw new IllegalStateException("Production SMTP must use STARTTLS or implicit TLS");
		}
	}

	private void requireCredentialsWhenSmtpAuthEnabled() {
		final boolean authentication = this.environment.getProperty(
				"spring.mail.properties.mail.smtp.auth", Boolean.class, false);
		if (authentication) {
			final String username = this.environment.getProperty("spring.mail.username");
			final String password = this.environment.getProperty("spring.mail.password");
			if (username == null || username.isBlank() || password == null || password.isBlank()) {
				throw new IllegalStateException("Production SMTP authentication requires credentials");
			}
		}
	}

	private void requireSharedRateLimits() {
		if (!this.rateLimitProperties.isEnabled()) {
			throw new IllegalStateException("Production authentication rate limits must be enabled");
		}
	}

	private void requireFreshFlywayInstallation() {
		if (this.environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, false)) {
			throw new IllegalStateException("Production Flyway baseline-on-migrate must be disabled");
		}
	}

	private static boolean isLoopback(String value) {
		final String lower = value.toLowerCase(Locale.ROOT);
		return lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("[::1]");
	}
}
