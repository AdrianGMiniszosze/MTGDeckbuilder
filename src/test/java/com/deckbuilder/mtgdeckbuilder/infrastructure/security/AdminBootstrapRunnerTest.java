package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.application.implement.PasswordHasher;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Bootstrap Runner Tests")
class AdminBootstrapRunnerTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordHasher passwordHasher;

	private BootstrapAdminProperties properties;

	private AdminBootstrapRunner runner;

	@BeforeEach
	void setUp() {
		this.properties = new BootstrapAdminProperties();
		this.runner = new AdminBootstrapRunner(this.properties, this.userRepository, this.passwordHasher);
	}

	@Test
	@DisplayName("Should do nothing when disabled")
	void shouldDoNothingWhenDisabled() {
		this.runner.run(new DefaultApplicationArguments());

		verify(this.userRepository, never()).existsByRole(any());
		verify(this.userRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should keep the existing administrator")
	void shouldKeepExistingAdmin() {
		this.properties.setEnabled(true);
		when(this.userRepository.existsByRole("ADMIN")).thenReturn(true);

		this.runner.run(new DefaultApplicationArguments());

		verify(this.userRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should skip an incomplete configuration")
	void shouldSkipIncompleteConfiguration() {
		this.properties.setEnabled(true);
		this.properties.setUsername(" ");
		when(this.userRepository.existsByRole("ADMIN")).thenReturn(false);

		this.runner.run(new DefaultApplicationArguments());

		verify(this.userRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should skip when the email is missing")
	void shouldSkipWithoutEmail() {
		this.properties.setEnabled(true);
		this.properties.setEmail(null);
		when(this.userRepository.existsByRole("ADMIN")).thenReturn(false);

		this.runner.run(new DefaultApplicationArguments());

		verify(this.userRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should skip when the password is missing")
	void shouldSkipWithoutPassword() {
		this.properties.setEnabled(true);
		this.properties.setEmail("admin@example.com");
		this.properties.setPassword("  ");
		when(this.userRepository.existsByRole("ADMIN")).thenReturn(false);

		this.runner.run(new DefaultApplicationArguments());

		verify(this.userRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should create the first administrator")
	void shouldCreateAdmin() {
		this.properties.setEnabled(true);
		this.properties.setUsername("root");
		this.properties.setEmail("root@example.com");
		this.properties.setPassword("secret");
		this.properties.setName("Root");
		this.properties.setCountry("ES");
		when(this.userRepository.existsByRole("ADMIN")).thenReturn(false);
		when(this.passwordHasher.hash("secret")).thenReturn("$2a$10$hash");

		this.runner.run(new DefaultApplicationArguments());

		final ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
		verify(this.userRepository).save(captor.capture());
		final UserEntity admin = captor.getValue();
		assertThat(admin.getUsername()).isEqualTo("root");
		assertThat(admin.getEmail()).isEqualTo("root@example.com");
		assertThat(admin.getName()).isEqualTo("Root");
		assertThat(admin.getCountry()).isEqualTo("ES");
		assertThat(admin.getRole()).isEqualTo("ADMIN");
		assertThat(admin.getIsActive()).isTrue();
		assertThat(admin.getHashedPassword()).isEqualTo("$2a$10$hash");
	}
}
