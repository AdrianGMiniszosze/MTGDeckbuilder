package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT configuration. The secret must be at least 32 bytes because HS256 needs a
 * 256-bit key; {@link JwtService} validates that lazily so the importer profile
 * can run without authentication.
 */
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
@Data
public class JwtProperties {

	/** HMAC-SHA256 secret. Required in web profiles. */
	private String secret;

	/** Access token lifetime; short because it cannot be revoked. */
	private Duration accessTokenTtl = Duration.ofMinutes(15);

	/** Refresh token lifetime. */
	private Duration refreshTokenTtl = Duration.ofDays(7);

	/** Value written to the {@code iss} claim and validated on decode. */
	private String issuer = "mtgdeckbuilder";
}
