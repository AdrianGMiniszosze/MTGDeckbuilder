package com.deckbuilder.mtgdeckbuilder.infrastructure.config;

import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthEmailProperties;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthRateLimitProperties;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.CorsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Production Configuration Validator Tests")
class ProductionConfigurationValidatorTest {

	@Test
	@DisplayName("Should accept a complete secure production configuration")
	void shouldAcceptProductionConfiguration() {
		final Fixture fixture = fixture();
		final ProductionConfigurationValidator validator = validator(fixture);
		assertThatCode(validator::validate).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should reject missing or loopback database URLs")
	void shouldRejectLocalDatabase() {
		assertInvalid(environment -> environment.setProperty("spring.datasource.url", ""));
		assertInvalid(environment -> environment.setProperty("spring.datasource.url", "jdbc:postgresql://localhost/db"));
		assertInvalid(environment -> environment.setProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1/db"));
		assertInvalid(environment -> environment.setProperty("spring.datasource.url", "jdbc:postgresql://[::1]/db"));
		assertInvalid(environment -> removeProperty(environment, "spring.datasource.url"));
	}

	@Test
	@DisplayName("Should require HTTPS for the public frontend")
	void shouldRequireHttpsFrontend() {
		assertInvalid((environment, email, limits, cors) -> email.setFrontendBaseUrl("http://app.example.com"));
		assertInvalid((environment, email, limits, cors) -> email.setFrontendBaseUrl(null));
	}

	@Test
	@DisplayName("Should require explicit HTTPS CORS origins")
	void shouldRequireSecureCorsOrigins() {
		assertInvalid((environment, email, limits, cors) -> cors.setAllowedOrigins(List.of()));
		assertInvalid((environment, email, limits, cors) -> cors.setAllowedOrigins(null));
		assertInvalid((environment, email, limits, cors) -> cors.setAllowedOrigins(List.of("http://app.example.com")));
		assertInvalid((environment, email, limits, cors) -> cors.setAllowedOrigins(Collections.singletonList(null)));
	}

	@Test
	@DisplayName("Should include the frontend link origin in CORS")
	void shouldMatchFrontendAndCorsOrigins() {
		assertInvalid((environment, email, limits, cors) ->
				cors.setAllowedOrigins(List.of("https://other.example.com")));
	}

	@Test
	@DisplayName("Should reject local mail sinks in production")
	void shouldRequireRemoteSmtp() {
		assertInvalid(environment -> removeProperty(environment, "spring.mail.host"));
		assertInvalid(environment -> environment.setProperty("spring.mail.host", "localhost"));
		assertInvalid(environment -> environment.setProperty("spring.mail.host", "127.0.0.1"));
		assertInvalid(environment -> environment.setProperty("spring.mail.host", "mailpit"));
		assertInvalid(environment -> environment.setProperty("spring.mail.host", " "));
	}

	@Test
	@DisplayName("Should require encrypted SMTP")
	void shouldRequireSmtpEncryption() {
		assertInvalid(environment -> {
			environment.setProperty("spring.mail.properties.mail.smtp.starttls.enable", "false");
			environment.setProperty("spring.mail.properties.mail.smtp.ssl.enable", "false");
		});
		assertThatCode(() -> assertValid(environment -> {
			environment.setProperty("spring.mail.properties.mail.smtp.starttls.enable", "false");
			environment.setProperty("spring.mail.properties.mail.smtp.ssl.enable", "true");
		})).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should require credentials when SMTP auth is enabled")
	void shouldRequireSmtpCredentials() {
		assertInvalid(environment -> environment.setProperty("spring.mail.username", " "));
		assertInvalid(environment -> removeProperty(environment, "spring.mail.username"));
		assertInvalid(environment -> environment.setProperty("spring.mail.password", " "));
		assertInvalid(environment -> removeProperty(environment, "spring.mail.password"));
		assertThatCode(() -> assertValid(environment -> {
			environment.setProperty("spring.mail.properties.mail.smtp.auth", "false");
			environment.setProperty("spring.mail.username", "");
			environment.setProperty("spring.mail.password", "");
		})).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should require enabled limits and a fresh Flyway install")
	void shouldRequireLimitsAndNoAutoBaseline() {
		assertInvalid((environment, email, limits, cors) -> limits.setEnabled(false));
		assertInvalid(environment -> environment.setProperty("spring.flyway.baseline-on-migrate", "true"));
	}

	private static void assertInvalid(EnvironmentChange change) {
		final Fixture fixture = fixture();
		change.apply(fixture.environment());
		assertThatThrownBy(validator(fixture)::validate).isInstanceOf(IllegalStateException.class);
	}

	private static void assertValid(EnvironmentChange change) {
		final Fixture fixture = fixture();
		change.apply(fixture.environment());
		validator(fixture).validate();
	}

	private static void assertInvalid(BeanChange change) {
		final Fixture fixture = fixture();
		change.apply(fixture.environment(), fixture.email(), fixture.limits(), fixture.cors());
		assertThatThrownBy(validator(fixture)::validate).isInstanceOf(IllegalStateException.class);
	}

	private static ProductionConfigurationValidator validator(Fixture fixture) {
		return new ProductionConfigurationValidator(fixture.environment(), fixture.email(),
				fixture.limits(), fixture.cors());
	}

	private static Fixture fixture() {
		final MockEnvironment environment = new MockEnvironment()
				.withProperty("spring.datasource.url", "jdbc:postgresql://db.example.com/mtg")
				.withProperty("spring.mail.host", "smtp.example.com")
				.withProperty("spring.mail.properties.mail.smtp.auth", "true")
				.withProperty("spring.mail.properties.mail.smtp.starttls.enable", "true")
				.withProperty("spring.mail.username", "smtp-user")
				.withProperty("spring.mail.password", "smtp-password")
				.withProperty("spring.flyway.baseline-on-migrate", "false");
		final AuthEmailProperties email = new AuthEmailProperties();
		email.setFrontendBaseUrl("https://app.example.com");
		final AuthRateLimitProperties limits = new AuthRateLimitProperties();
		final CorsProperties cors = new CorsProperties();
		cors.setAllowedOrigins(List.of("https://app.example.com"));
		return new Fixture(environment, email, limits, cors);
	}

	@SuppressWarnings("unchecked")
	private static void removeProperty(MockEnvironment environment, String name) {
		((MapPropertySource) environment.getPropertySources().get("mockProperties")).getSource().remove(name);
	}

	private record Fixture(MockEnvironment environment, AuthEmailProperties email,
			AuthRateLimitProperties limits, CorsProperties cors) {
	}

	@FunctionalInterface
	private interface EnvironmentChange {
		void apply(MockEnvironment environment);
	}

	@FunctionalInterface
	private interface BeanChange {
		void apply(MockEnvironment environment, AuthEmailProperties email,
				AuthRateLimitProperties limits, CorsProperties cors);
	}
}
