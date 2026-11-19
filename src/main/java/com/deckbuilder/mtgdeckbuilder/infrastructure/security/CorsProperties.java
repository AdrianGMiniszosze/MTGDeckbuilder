package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Exact browser origins. Empty by default, so same-origin/native clients need
 * no CORS exception.
 */
@Component
@ConfigurationProperties(prefix = "app.security.cors")
@Data
public class CorsProperties {

	private List<String> allowedOrigins = new ArrayList<>();

	@PostConstruct
	void validate() {
		if (this.allowedOrigins == null) {
			throw new IllegalArgumentException("CORS allowed origins must be a list");
		}
		for (String origin : this.allowedOrigins) {
			if (origin == null || origin.isBlank() || "*".equals(origin)) {
				throw new IllegalArgumentException("CORS origins must be explicit HTTP(S) origins");
			}
			final URI uri;
			try {
				uri = URI.create(origin);
			} catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException("CORS origin is not a valid URI", exception);
			}
			if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					|| uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
					|| uri.getRawFragment() != null || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
				throw new IllegalArgumentException("CORS entries must be exact HTTP(S) origins without paths");
			}
		}
	}
}
