package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.implement.PasswordHasher;
import com.deckbuilder.mtgdeckbuilder.infrastructure.RefreshTokenRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentication API Integration Tests")
@AutoConfigureMockMvc
class AuthIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private ObjectMapper objectMapper;

	private UserEntity user;

	@AfterEach
	void cleanUp() {
		this.userRepository.findByUsername("janedoe").ifPresent(this.userRepository::delete);
		if (this.user != null) {
			this.userRepository.deleteById(this.user.getId());
		}
	}

	@BeforeEach
	void setUp() {
		final UserEntity entity = new UserEntity();
		entity.setName("John Doe");
		entity.setUsername("johndoe");
		entity.setEmail("john@example.com");
		entity.setCountry("USA");
		entity.setHashedPassword(this.passwordHasher.hash("secret-password"));
		entity.setRole("USER");
		entity.setIsActive(true);
		entity.setCreatedAt(LocalDateTime.now());
		this.user = this.userRepository.save(entity);
	}

	@Test
	@DisplayName("Should register a new user with a hashed password")
	void shouldRegister() throws Exception {
		this.mockMvc
			.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
					{"name":"Jane Doe","username":"janedoe","email":"jane@example.com","country":"USA",
					 "password":"another-secret"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value("janedoe"))
			.andExpect(jsonPath("$.hashed_password").doesNotExist());

		final UserEntity created = this.userRepository.findByUsername("janedoe").orElseThrow();
		assertThat(created.getRole()).isEqualTo("USER");
		assertThat(created.getIsActive()).isTrue();
		assertThat(created.getHashedPassword()).isNotEqualTo("another-secret");
		assertThat(this.passwordHasher.matches("another-secret", created.getHashedPassword())).isTrue();
	}

	@Test
	@DisplayName("Should reject a duplicate username")
	void shouldRejectDuplicateUsername() throws Exception {
		this.mockMvc
			.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
					{"name":"Other","username":"johndoe","email":"other@example.com","country":"USA",
					 "password":"another-secret"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"));
	}

	@Test
	@DisplayName("Should reject a short password")
	void shouldRejectShortPassword() throws Exception {
		this.mockMvc
			.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
					{"name":"Short","username":"shorty","email":"short@example.com","country":"USA",
					 "password":"123"}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should log in and return a token pair")
	void shouldLogin() throws Exception {
		this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"johndoe\",\"password\":\"secret-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").isNotEmpty())
			.andExpect(jsonPath("$.refresh_token").isNotEmpty())
			.andExpect(jsonPath("$.token_type").value("Bearer"))
			.andExpect(jsonPath("$.expires_in").value(900));

		assertThat(this.userRepository.findById(this.user.getId()).orElseThrow().getLastLogin()).isNotNull();
	}

	@Test
	@DisplayName("Should log in with the email address")
	void shouldLoginWithEmail() throws Exception {
		this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"john@example.com\",\"password\":\"secret-password\"}"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Should reject a wrong password")
	void shouldRejectWrongPassword() throws Exception {
		this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"johndoe\",\"password\":\"nope\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("Should reject an unknown user without revealing it")
	void shouldRejectUnknownUser() throws Exception {
		this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"ghost\",\"password\":\"nope\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("Should reject a disabled account")
	void shouldRejectDisabledAccount() throws Exception {
		this.user.setIsActive(false);
		this.userRepository.save(this.user);

		this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"johndoe\",\"password\":\"secret-password\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
	}

	@Test
	@DisplayName("Should revoke rotated sessions when account is disabled")
	void shouldRevokeSessionsForDisabledAccount() throws Exception {
		final String refresh = login().get("refresh_token").asText();
		this.user.setIsActive(false);
		this.userRepository.save(this.user);

		this.mockMvc
			.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refresh_token\":\"" + refresh + "\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));

		assertThat(this.refreshTokenRepository.findAll())
			.filteredOn(token -> token.getUserId().equals(this.user.getId()))
			.allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
	}

	@Test
	@DisplayName("Should rotate the refresh token and reject its reuse")
	void shouldRotateRefreshToken() throws Exception {
		final JsonNode tokens = login();
		final String refresh = tokens.get("refresh_token").asText();

		final String rotated = this.mockMvc
			.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refresh_token\":\"" + refresh + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").isNotEmpty())
			.andReturn().getResponse().getContentAsString();

		final String newRefresh = this.objectMapper.readTree(rotated).get("refresh_token").asText();
		assertThat(newRefresh).isNotEqualTo(refresh);

		// Replaying the old token is treated as theft and revokes every session.
		this.mockMvc
			.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refresh_token\":\"" + refresh + "\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));

		this.mockMvc
			.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refresh_token\":\"" + newRefresh + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Should revoke the refresh token on logout")
	void shouldLogout() throws Exception {
		final String refresh = login().get("refresh_token").asText();

		this.mockMvc
			.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refresh_token\":\"" + refresh + "\"}"))
			.andExpect(status().isNoContent());

		this.mockMvc
			.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refresh_token\":\"" + refresh + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Should return the current user for a valid access token")
	void shouldReturnCurrentUser() throws Exception {
		final String access = login().get("access_token").asText();

		this.mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + access))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("johndoe"))
			.andExpect(jsonPath("$.hashed_password").doesNotExist());
	}

	@Test
	@DisplayName("Should reject /auth/me without or with a bad token")
	void shouldRejectCurrentUserWithoutToken() throws Exception {
		this.mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		this.mockMvc.perform(get("/auth/me").header("Authorization", "Bearer not-a-token"))
			.andExpect(status().isUnauthorized());
	}

	private JsonNode login() throws Exception {
		final String body = this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"johndoe\",\"password\":\"secret-password\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return this.objectMapper.readTree(body);
	}
}
