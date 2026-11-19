package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagId;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.TagEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Card Tag API Integration Tests")
@Transactional
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class CardTagIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private CardTagRepository cardTagRepository;

	private CardEntity card;
	private TagEntity removal;
	private TagEntity ramp;

	@BeforeEach
	void setUp() {
		this.card = this.cardRepository.save(createCard("Lightning Bolt", "Instant"));
		this.removal = saveTag("removal");
		this.ramp = saveTag("ramp");
	}

	@Test
	@DisplayName("Should return an empty list when the card has no tags")
	void shouldListEmptyTags() throws Exception {
		this.mockMvc.perform(get("/cards/{id}/tags", this.card.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	@DisplayName("Should assign a tag with the default scores")
	void shouldCreateTagWithDefaults() throws Exception {
		this.mockMvc
			.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tag_id\":" + this.removal.getId() + "}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.card_id").value(this.card.getId()))
			.andExpect(jsonPath("$.tag_id").value(this.removal.getId()))
			.andExpect(jsonPath("$.weight").value(1.0))
			.andExpect(jsonPath("$.confidence").value(1.0))
			.andExpect(jsonPath("$.source").value("manual"));

		final var persisted = this.cardTagRepository.findById(new CardTagId(this.card.getId(), this.removal.getId()));
		assertThat(persisted).isPresent();
		assertThat(persisted.get().getWeight()).isEqualTo(1.0);
		assertThat(persisted.get().getConfidence()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Should keep the scores provided when assigning a tag")
	void shouldCreateTagWithExplicitScores() throws Exception {
		this.mockMvc
			.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tag_id\":" + this.ramp.getId() + ",\"weight\":2.5,\"confidence\":0.4}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.weight").value(2.5))
			.andExpect(jsonPath("$.confidence").value(0.4));
	}

	@Test
	@DisplayName("Should list the tags of a card after assigning them")
	void shouldListAssignedTags() throws Exception {
		assign("{\"tag_id\":" + this.removal.getId() + "}");
		assign("{\"tag_id\":" + this.ramp.getId() + ",\"weight\":2.0}");

		this.mockMvc.perform(get("/cards/{id}/tags", this.card.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[*].tag_id").value(org.hamcrest.Matchers.containsInAnyOrder(
					this.removal.getId().intValue(), this.ramp.getId().intValue())));
	}

	@Test
	@DisplayName("Should reject assigning the same tag twice with 409")
	void shouldRejectDuplicateTag() throws Exception {
		assign("{\"tag_id\":" + this.removal.getId() + "}");

		this.mockMvc
			.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tag_id\":" + this.removal.getId() + "}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("CARD_TAG_EXISTS"));
	}

	@Test
	@DisplayName("Should return 404 when the card does not exist")
	void shouldRejectUnknownCard() throws Exception {
		this.mockMvc
			.perform(post("/cards/{id}/tags", 999_999L).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tag_id\":" + this.removal.getId() + "}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("CARD_NOT_FOUND"));
	}

	@Test
	@DisplayName("Should return 404 when the tag does not exist")
	void shouldRejectUnknownTag() throws Exception {
		this.mockMvc
			.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tag_id\":999999}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("TAG_NOT_FOUND"));
	}

	@Test
	@DisplayName("Should return 400 when confidence is out of range")
	void shouldRejectOutOfRangeConfidence() throws Exception {
		this.mockMvc
			.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tag_id\":" + this.removal.getId() + ",\"confidence\":1.5}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_CARD_TAG"));
	}

	@Test
	@DisplayName("Should return 400 when tag_id is missing")
	void shouldRejectMissingTagId() throws Exception {
		this.mockMvc
			.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));
	}

	@Test
	@DisplayName("Should update only the provided field of a tag")
	void shouldUpdateTagPartially() throws Exception {
		assign("{\"tag_id\":" + this.removal.getId() + ",\"weight\":1.0,\"confidence\":0.5}");

		this.mockMvc
			.perform(put("/cards/{id}/tags/{tagId}", this.card.getId(), this.removal.getId())
				.contentType(MediaType.APPLICATION_JSON).content("{\"weight\":3.0}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.weight").value(3.0))
			.andExpect(jsonPath("$.confidence").value(0.5));

		final var persisted = this.cardTagRepository.findById(new CardTagId(this.card.getId(), this.removal.getId()));
		assertThat(persisted).isPresent();
		assertThat(persisted.get().getWeight()).isEqualTo(3.0);
		assertThat(persisted.get().getConfidence()).isEqualTo(0.5);
	}

	@Test
	@DisplayName("Should return 400 when the update has no field")
	void shouldRejectEmptyUpdate() throws Exception {
		assign("{\"tag_id\":" + this.removal.getId() + "}");

		this.mockMvc
			.perform(put("/cards/{id}/tags/{tagId}", this.card.getId(), this.removal.getId())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_CARD_TAG"))
			.andExpect(jsonPath("$.message").value(containsString("weight or confidence")));
	}

	@Test
	@DisplayName("Should return 404 when updating a missing card tag")
	void shouldReturn404WhenUpdatingMissingTag() throws Exception {
		this.mockMvc
			.perform(put("/cards/{id}/tags/{tagId}", this.card.getId(), this.removal.getId())
				.contentType(MediaType.APPLICATION_JSON).content("{\"weight\":2.0}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("CARD_TAG_NOT_FOUND"));
	}

	@Test
	@DisplayName("Should delete a card tag and be idempotent")
	void shouldDeleteTag() throws Exception {
		assign("{\"tag_id\":" + this.removal.getId() + "}");

		this.mockMvc.perform(delete("/cards/{id}/tags/{tagId}", this.card.getId(), this.removal.getId()))
			.andExpect(status().isNoContent());
		assertThat(this.cardTagRepository.findById(new CardTagId(this.card.getId(), this.removal.getId()))).isEmpty();

		this.mockMvc.perform(delete("/cards/{id}/tags/{tagId}", this.card.getId(), this.removal.getId()))
			.andExpect(status().isNoContent());
	}

	private void assign(String body) throws Exception {
		this.mockMvc.perform(post("/cards/{id}/tags", this.card.getId()).contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isCreated());
	}

	private TagEntity saveTag(String name) {
		final TagEntity tag = new TagEntity();
		tag.setName(name);
		return this.tagRepository.save(tag);
	}

	private CardEntity createCard(String name, String cardType) {
		final CardEntity entity = new CardEntity();
		entity.setName(name);
		entity.setManaCost("{R}");
		entity.setCmc(1);
		entity.setColorIdentity("R");
		entity.setTypeLine(cardType);
		entity.setCardType(cardType);
		entity.setRarity("common");
		entity.setCardText("Test card text");
		entity.setImageUrl("http://example.com/image.jpg");
		entity.setLanguage("en");
		entity.setCollectorNumber("001");
		entity.setFoil(false);
		entity.setGameChanger(false);
		entity.setPromo(false);
		entity.setVariation(false);
		return this.cardRepository.save(entity);
	}
}
