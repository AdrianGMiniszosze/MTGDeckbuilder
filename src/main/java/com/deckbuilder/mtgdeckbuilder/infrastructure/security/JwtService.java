package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.model.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Issues and decodes the short-lived access token. The refresh token is opaque
 * and lives in the database instead of the JWT.
 */
@Component
@RequiredArgsConstructor
public class JwtService {

	private static final int MIN_SECRET_BYTES = 32;

	private final JwtProperties properties;

	public String issueAccessToken(User user) {
		final Instant now = Instant.now();
		final JwtClaimsSet claims = JwtClaimsSet.builder().issuer(this.properties.getIssuer())
				.subject(user.getUsername()).issuedAt(now).expiresAt(now.plus(this.properties.getAccessTokenTtl()))
				.claim("uid", user.getId()).claim("role", user.getRole() != null ? user.getRole() : "USER").build();

		final NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	public JwtDecoder decoder() {
		final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey())
				.macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(this.properties.getIssuer()));
		return decoder;
	}

	public long accessTokenSeconds() {
		return this.properties.getAccessTokenTtl().toSeconds();
	}

	private SecretKey secretKey() {
		final String secret = this.properties.getSecret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"app.security.jwt.secret must be provided and at least " + MIN_SECRET_BYTES + " bytes long");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
