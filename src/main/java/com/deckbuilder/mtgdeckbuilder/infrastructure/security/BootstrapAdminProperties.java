package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * First administrator provisioning. Disabled by default: enable it once, start
 * the application with {@code app.security.bootstrap-admin.enabled=true} and the
 * given credentials, then turn it off again. The runner only acts when no admin
 * exists, so leaving it enabled cannot overwrite an account.
 */
@Component
@ConfigurationProperties(prefix = "app.security.bootstrap-admin")
@Data
public class BootstrapAdminProperties {

	private boolean enabled;

	private String username = "admin";

	private String email;

	private String password;

	private String name = "Administrator";

	private String country = "US";
}
