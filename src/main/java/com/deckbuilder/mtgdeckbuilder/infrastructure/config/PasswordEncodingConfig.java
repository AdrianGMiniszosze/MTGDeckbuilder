package com.deckbuilder.mtgdeckbuilder.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing lives behind Spring's {@link PasswordEncoder} so the
 * application code never picks a raw crypto primitive.
 */
@Configuration
public class PasswordEncodingConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
