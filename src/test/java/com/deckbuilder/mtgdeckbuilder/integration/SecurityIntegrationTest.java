package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.implement.PasswordHasher;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthRateLimitEndpoint;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.AuthRateLimitMetrics;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authorization and Visibility Integration Tests")
@Transactional
@AutoConfigureMockMvc
class SecurityIntegrationTest extends PostgresIntegrationTest {

	private static final String PASSWORD = "secret-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DeckRepository deckRepository;

	@Autowired
	private FormatRepository formatRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private AuthRateLimitMetrics authRateLimitMetrics;

	@Autowired
	private ObjectMapper objectMapper;

	private UserEntity owner;
	private UserEntity other;
	private UserEntity admin;
	private FormatEntity format;
	private DeckEntity privateDraft;
	private DeckEntity publicDeck;

	@BeforeEach
	void setUp() {
		this.owner = saveUser("owner", "owner@example.com", "USER");
		this.other = saveUser("other", "other@example.com", "USER");
		this.admin = saveUser("root", "root@example.com", "ADMIN");
		this.format = this.formatRepository.save(FormatEntity.builder().name("Security Test Format")
			.description("Fixture").minDeckSize(60).maxSideboardSize(15).build());
		this.privateDraft = saveDeck("Owner draft", this.owner, "draft", true);
		this.publicDeck = saveDeck("Owner public", this.owner, "published", false);
		saveDeck("Other draft", this.other, "draft", true);
	}

	@Test
	@DisplayName("Should list only public published decks for anonymous callers")
	void shouldListPublicDecksAnonymously() throws Exception {
		this.mockMvc.perform(get("/decks"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItem(this.publicDeck.getId().intValue())))
			.andExpect(jsonPath("$[*].id", not(hasItem(this.privateDraft.getId().intValue()))));
	}

	@Test
	@DisplayName("Should hide a private draft from anonymous and other users")
	void shouldHidePrivateDraft() throws Exception {
		this.mockMvc.perform(get("/decks/{id}", this.privateDraft.getId())).andExpect(status().isNotFound());

		this.mockMvc.perform(get("/decks/{id}", this.privateDraft.getId()).header("Authorization", bearer("other")))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Should show a public deck to anonymous callers")
	void shouldShowPublicDeck() throws Exception {
		this.mockMvc.perform(get("/decks/{id}", this.publicDeck.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deck_name").value("Owner public"));
		this.mockMvc.perform(get("/decks/{id}/validation", this.publicDeck.getId()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Should let the owner and admins read a private draft")
	void shouldAllowOwnerAndAdminToReadDraft() throws Exception {
		this.mockMvc.perform(get("/decks/{id}", this.privateDraft.getId()).header("Authorization", bearer("owner")))
			.andExpect(status().isOk());

		this.mockMvc.perform(get("/decks/{id}", this.privateDraft.getId()).header("Authorization", bearer("root")))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Should hide the validation of someone else's draft")
	void shouldHideValidationOfOtherDraft() throws Exception {
		this.mockMvc.perform(get("/decks/{id}/validation", this.privateDraft.getId()))
			.andExpect(status().isNotFound());
		this.mockMvc
			.perform(get("/decks/{id}/validation", this.privateDraft.getId()).header("Authorization", bearer("other")))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Should answer 403 when writing another user's public deck")
	void shouldForbidWritingSomeoneElsesPublicDeck() throws Exception {
		this.mockMvc
			.perform(put("/decks/{id}", this.publicDeck.getId()).header("Authorization", bearer("other"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"deck_name\":\"Hijacked\",\"format\":" + this.format.getId() + "}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("Should let the owner update their own draft")
	void shouldLetOwnerUpdateDraft() throws Exception {
		this.mockMvc
			.perform(put("/decks/{id}", this.privateDraft.getId()).header("Authorization", bearer("owner"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"deck_name\":\"Renamed\",\"user_id\":" + this.other.getId() + ",\"format\":"
						+ this.format.getId() + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deck_name").value("Renamed"));
		assertThat(this.deckRepository.findById(this.privateDraft.getId()).orElseThrow().getUserId())
			.isEqualTo(this.owner.getId());
	}

	@Test
	@DisplayName("Should derive the owner of a new deck from the token")
	void shouldDeriveDeckOwner() throws Exception {
		final String body = this.mockMvc
			.perform(post("/decks").header("Authorization", bearer("owner")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"deck_name\":\"New deck\",\"user_id\":" + this.other.getId() + ",\"format\":"
						+ this.format.getId() + "}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		final long createdId = this.objectMapper.readTree(body).get("id").asLong();
		assertThat(this.deckRepository.findById(createdId).orElseThrow().getUserId()).isEqualTo(this.owner.getId());
	}

	@Test
	@DisplayName("Should require admin to list users")
	void shouldProtectUserListing() throws Exception {
		this.mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/users").header("Authorization", bearer("owner"))).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/users").header("Authorization", bearer("root"))).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Should let a user read only their own profile and decks")
	void shouldProtectUserResources() throws Exception {
		this.mockMvc.perform(get("/users/{id}", this.owner.getId()).header("Authorization", bearer("owner")))
			.andExpect(status().isOk());
		this.mockMvc.perform(get("/users/{id}", this.owner.getId()).header("Authorization", bearer("other")))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(get("/users/{id}/decks", this.owner.getId()).header("Authorization", bearer("owner")))
			.andExpect(status().isOk());
		this.mockMvc.perform(get("/users/{id}/decks", this.owner.getId()).header("Authorization", bearer("other")))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Should require admin to delete users")
	void shouldProtectUserDeletion() throws Exception {
		final UserEntity disposable = saveUser("disposable", "disposable@example.com", "USER");

		this.mockMvc.perform(delete("/users/{id}", disposable.getId()).header("Authorization", bearer("owner")))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(delete("/users/{id}", disposable.getId()).header("Authorization", bearer("root")))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Should require admin to create tags and formats")
	void shouldProtectReferenceWrites() throws Exception {
		this.mockMvc.perform(post("/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"mill\"}"))
			.andExpect(status().isUnauthorized());
		this.mockMvc
			.perform(post("/tags").header("Authorization", bearer("owner")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"mill\"}"))
			.andExpect(status().isForbidden());
		this.mockMvc
			.perform(post("/tags").header("Authorization", bearer("root")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"mill\"}"))
			.andExpect(status().isCreated());

		this.mockMvc
			.perform(post("/formats").header("Authorization", bearer("owner")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"format_name\":\"Test format\"}"))
			.andExpect(status().isForbidden());
		this.mockMvc
			.perform(post("/formats").header("Authorization", bearer("root")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"format_name\":\"Test format\"}"))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Should keep card reads public")
	void shouldKeepCardReadsPublic() throws Exception {
		this.mockMvc.perform(get("/cards")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Should expose health publicly and restrict metrics to administrators")
	void shouldProtectActuatorMetrics() throws Exception {
		this.authRateLimitMetrics.record(AuthRateLimitEndpoint.LOGIN, true);

		this.mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		this.mockMvc.perform(get("/actuator/metrics/auth.rate_limit.allowed"))
			.andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/actuator/metrics/auth.rate_limit.allowed")
				.header("Authorization", bearer("root")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("auth.rate_limit.allowed"));
	}

	private UserEntity saveUser(String username, String email, String role) {
		final UserEntity entity = new UserEntity();
		entity.setName(username);
		entity.setUsername(username);
		entity.setEmail(email);
		entity.setCountry("USA");
		entity.setHashedPassword(this.passwordHasher.hash(PASSWORD));
		entity.setRole(role);
		entity.setIsActive(true);
		entity.setCreatedAt(LocalDateTime.now());
		return this.userRepository.save(entity);
	}

	private DeckEntity saveDeck(String name, UserEntity user, String status, boolean isPrivate) {
		final DeckEntity deck = new DeckEntity();
		deck.setName(name);
		deck.setUserId(user.getId());
		deck.setFormatId(this.format.getId());
		deck.setStatus(status);
		deck.setIsPrivate(isPrivate);
		deck.setCreated(LocalDateTime.now());
		deck.setModified(LocalDateTime.now());
		return this.deckRepository.save(deck);
	}

	private String bearer(String username) throws Exception {
		final String body = this.mockMvc
			.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return "Bearer " + this.objectMapper.readTree(body).get("access_token").asText();
	}
}
