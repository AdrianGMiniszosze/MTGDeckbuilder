package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.application.implement.PasswordHasher;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator so the admin-only endpoints are reachable on
 * a fresh database. It never modifies or promotes existing accounts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AdminBootstrapRunner implements ApplicationRunner {

	private final BootstrapAdminProperties properties;
	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!this.properties.isEnabled() || this.userRepository.existsByRole("ADMIN")) {
			return;
		}
		if (isBlank(this.properties.getUsername()) || isBlank(this.properties.getEmail())
				|| isBlank(this.properties.getPassword())) {
			log.warn("Bootstrap admin is enabled but username, email or password is missing; skipping");
			return;
		}

		final UserEntity admin = new UserEntity();
		admin.setName(this.properties.getName());
		admin.setUsername(this.properties.getUsername());
		admin.setEmail(this.properties.getEmail());
		admin.setCountry(this.properties.getCountry());
		admin.setHashedPassword(this.passwordHasher.hash(this.properties.getPassword()));
		admin.setRole("ADMIN");
		admin.setIsActive(true);
		this.userRepository.save(admin);
		log.info("Bootstrapped administrator '{}'", admin.getUsername());
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
