package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Controller Tests")
class CardControllerTest {

	@Mock
	private CardService cardService;

	@Mock
	private CardTagService cardTagService;

	@Mock
	private CardMapper cardMapper;

	@Mock
	private CardTagMapper cardTagMapper;

	@InjectMocks
	private CardController cardController;

	private Card testCard;
	private CardDTO testCardDTO;

	@BeforeEach
	void setUp() {
		this.testCard = Card.builder().id(1L).name("Lightning Bolt").manaCost("{R}").typeLine("Instant")
				.oracleText("Lightning Bolt deals 3 damage to any target.").rarity("Common").build();

		this.testCardDTO = CardDTO.builder().id(1).card_name("Lightning Bolt").mana_cost("{R}").cmc(1)
				.color_identity("R").type_line("Instant").card_type("Instant").rarity(CardDTO.Rarity.COMMON)
				.card_text("Lightning Bolt deals 3 damage to any target.").image_url("https://example.com/card.jpg")
				.language("en").build();
	}

	@Test
	@DisplayName("Should list all cards")
	void shouldListAllCards() {
		// Given
		final Card card2 = Card.builder().id(2L).name("Counterspell").build();
		final CardDTO cardDTO2 = CardDTO.builder().id(2).card_name("Counterspell").mana_cost("{U}{U}").cmc(2)
				.color_identity("U").type_line("Instant").card_type("Instant").rarity(CardDTO.Rarity.UNCOMMON)
				.card_text("Counter target spell.").image_url("https://example.com/counterspell.jpg").language("en")
				.build();

		when(this.cardService.getAllCards(10, 0)).thenReturn(Arrays.asList(this.testCard, card2));
		when(this.cardMapper.toDto(this.testCard)).thenReturn(this.testCardDTO);
		when(this.cardMapper.toDto(card2)).thenReturn(cardDTO2);

		// When
		final ResponseEntity<List<CardDTO>> response = this.cardController.listCards(10, 0);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getCard_name()).isEqualTo("Lightning Bolt");
		assertThat(response.getBody().get(1).getCard_name()).isEqualTo("Counterspell");
		verify(this.cardService, times(1)).getAllCards(10, 0);
		verify(this.cardMapper, times(2)).toDto(any(Card.class));
	}

	@Test
	@DisplayName("Should list cards with default pagination")
	void shouldListCardsWithDefaultPagination() {
		// Given
		when(this.cardService.getAllCards(10, 0)).thenReturn(List.of(this.testCard));
		when(this.cardMapper.toDto(this.testCard)).thenReturn(this.testCardDTO);

		// When
		final ResponseEntity<List<CardDTO>> response = this.cardController.listCards(null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.cardService, times(1)).getAllCards(10, 0);
	}

	@Test
	@DisplayName("Should get card by ID when exists")
	void shouldGetCardByIdWhenExists() {
		// Given
		when(this.cardService.getCardById(1L)).thenReturn(Optional.of(this.testCard));
		when(this.cardMapper.toDto(this.testCard)).thenReturn(this.testCardDTO);

		// When
		final ResponseEntity<CardDTO> response = this.cardController.getCardById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCard_name()).isEqualTo("Lightning Bolt");
		verify(this.cardService, times(1)).getCardById(1L);
		verify(this.cardMapper, times(1)).toDto(this.testCard);
	}

	@Test
	@DisplayName("Should throw exception when card not found")
	void shouldReturn404WhenCardNotFound() {

		// When/Then
		assertThatThrownBy(() -> this.cardController.getCardById(999)).isInstanceOf(CardNotFoundException.class)
				.hasMessageContaining("Card not found with id: 999");

		verify(this.cardService, times(1)).getCardById(999L);
		verify(this.cardMapper, never()).toDto(any(Card.class));
	}

	@Test
	@DisplayName("Should create card")
	void shouldCreateCard() {
		// Given
		final Card newCard = Card.builder().name("New Card").build();
		final Card createdCard = Card.builder().id(3L).name("New Card").build();

		final CardDTO newCardDTO = CardDTO.builder().card_name("New Card").mana_cost("{1}").cmc(1).color_identity("")
				.type_line("Artifact").card_type("Artifact").rarity(CardDTO.Rarity.COMMON).card_text("Test artifact")
				.image_url("https://example.com/new.jpg").language("en").build();

		final CardDTO createdCardDTO = CardDTO.builder().id(3).card_name("New Card").mana_cost("{1}").cmc(1)
				.color_identity("").type_line("Artifact").card_type("Artifact").rarity(CardDTO.Rarity.COMMON)
				.card_text("Test artifact").image_url("https://example.com/new.jpg").language("en").build();

		when(this.cardMapper.toEntity(newCardDTO)).thenReturn(newCard);
		when(this.cardService.createCard(newCard)).thenReturn(createdCard);
		when(this.cardMapper.toDto(createdCard)).thenReturn(createdCardDTO);

		// When
		final ResponseEntity<CardDTO> response = this.cardController.createCard(newCardDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(3);
		assertThat(response.getBody().getCard_name()).isEqualTo("New Card");
		verify(this.cardMapper, times(1)).toEntity(newCardDTO);
		verify(this.cardService, times(1)).createCard(newCard);
		verify(this.cardMapper, times(1)).toDto(createdCard);
	}

	@Test
	@DisplayName("Should update card when exists")
	void shouldUpdateCardWhenExists() {
		// Given
		final CardDTO updatedDTO = CardDTO.builder().id(1).card_name("Lightning Bolt Updated").mana_cost("{R}").cmc(1)
				.color_identity("R").type_line("Instant").card_type("Instant").rarity(CardDTO.Rarity.UNCOMMON)
				.card_text("Updated text").image_url("https://example.com/updated.jpg").language("en").build();

		final Card updatedCard = Card.builder().name("Lightning Bolt Updated").build();
		final Card savedCard = Card.builder().id(1L).name("Lightning Bolt Updated").build();

		final CardDTO savedDTO = CardDTO.builder().id(1).card_name("Lightning Bolt Updated").mana_cost("{R}").cmc(1)
				.color_identity("R").type_line("Instant").card_type("Instant").rarity(CardDTO.Rarity.UNCOMMON)
				.card_text("Updated text").image_url("https://example.com/updated.jpg").language("en").build();

		when(this.cardMapper.toEntity(updatedDTO)).thenReturn(updatedCard);
		when(this.cardService.updateCard(1L, updatedCard)).thenReturn(Optional.of(savedCard));
		when(this.cardMapper.toDto(savedCard)).thenReturn(savedDTO);

		// When
		final ResponseEntity<CardDTO> response = this.cardController.updateCard(1, updatedDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCard_name()).isEqualTo("Lightning Bolt Updated");
		verify(this.cardMapper, times(1)).toEntity(updatedDTO);
		verify(this.cardService, times(1)).updateCard(1L, updatedCard);
		verify(this.cardMapper, times(1)).toDto(savedCard);
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent card")
	void shouldReturn404WhenUpdatingNonExistentCard() {
		// Given
		final CardDTO updatedDTO = CardDTO.builder().card_name("Non-existent").mana_cost("{1}").cmc(1)
				.color_identity("").type_line("Artifact").card_type("Artifact").rarity(CardDTO.Rarity.COMMON)
				.card_text("Test").image_url("https://example.com/test.jpg").language("en").build();

		final Card card = Card.builder().name("Non-existent").build();

		when(this.cardMapper.toEntity(updatedDTO)).thenReturn(card);
		when(this.cardService.updateCard(999L, card)).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.cardController.updateCard(999, updatedDTO))
				.isInstanceOf(CardNotFoundException.class).hasMessageContaining("Card not found with id: 999");

		// Then
		verify(this.cardService, times(1)).updateCard(999L, card);
		verify(this.cardMapper, never()).toDto(any(Card.class));
	}

	@Test
	@DisplayName("Should delete card")
	void shouldDeleteCard() {
		// Given
		doNothing().when(this.cardService).deleteCard(1L);

		// When
		final ResponseEntity<Void> response = this.cardController.deleteCard(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(this.cardService, times(1)).deleteCard(1L);
	}

	@Test
	@DisplayName("Should list tags for card")
	void shouldListTagsForCard() {
		// Given
		final CardTag tag1 = CardTag.builder().cardId(1L).tagId(1L).weight(0.9).build();
		final CardTag tag2 = CardTag.builder().cardId(1L).tagId(2L).weight(0.8).build();
		final List<CardTag> tags = Arrays.asList(tag1, tag2);

		final CardTagDTO tagDTO1 = CardTagDTO.builder().card_id(1).tag_id(1).weight(0.9f).confidence(0.95f)
				.source(CardTagDTO.Source.MANUAL).build();

		final CardTagDTO tagDTO2 = CardTagDTO.builder().card_id(1).tag_id(2).weight(0.8f).confidence(0.90f)
				.source(CardTagDTO.Source.MANUAL).build();

		final List<CardTagDTO> tagDTOs = Arrays.asList(tagDTO1, tagDTO2);

		when(this.cardTagService.findByCardId(1L)).thenReturn(tags);
		when(this.cardTagMapper.toCardTagDTOs(tags)).thenReturn(tagDTOs);

		// When
		final ResponseEntity<List<CardTagDTO>> response = this.cardController.listTagsForCard(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		verify(this.cardTagService, times(1)).findByCardId(1L);
		verify(this.cardTagMapper, times(1)).toCardTagDTOs(tags);
	}

	@Test
	@DisplayName("Should update card tag when exists")
	void shouldUpdateCardTagWhenExists() {
		// Given
		final CardTagDTO tagDTO = CardTagDTO.builder().card_id(1).tag_id(1).weight(0.95f).confidence(0.99f)
				.source(CardTagDTO.Source.AI_TEXT).build();

		final CardTag tag = CardTag.builder().cardId(1L).tagId(1L).build();
		final CardTag updatedTag = CardTag.builder().cardId(1L).tagId(1L).weight(0.95).build();

		final CardTagDTO updatedDTO = CardTagDTO.builder().card_id(1).tag_id(1).weight(0.95f).confidence(0.99f)
				.source(CardTagDTO.Source.AI_TEXT).build();

		when(this.cardTagMapper.toCardTag(tagDTO)).thenReturn(tag);
		when(this.cardTagService.updateCardTag(1L, 1L, tag)).thenReturn(Optional.of(updatedTag));
		when(this.cardTagMapper.toCardTagDTO(updatedTag)).thenReturn(updatedDTO);

		// When
		final ResponseEntity<CardTagDTO> response = this.cardController.updateCardTag(1, 1, tagDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.cardTagService, times(1)).updateCardTag(1L, 1L, tag);
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent card tag")
	void shouldReturn404WhenUpdatingNonExistentCardTag() {
		// Given
		final CardTagDTO tagDTO = CardTagDTO.builder().card_id(999).tag_id(999).weight(0.5f).confidence(0.5f)
				.source(CardTagDTO.Source.MANUAL).build();

		final CardTag tag = CardTag.builder().build();

		when(this.cardTagMapper.toCardTag(tagDTO)).thenReturn(tag);
		when(this.cardTagService.updateCardTag(999L, 999L, tag)).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.cardController.updateCardTag(999, 999, tagDTO))
				.isInstanceOf(CardNotFoundException.class).hasMessageContaining("Card not found with id: 999");

		// Then
		verify(this.cardTagService, times(1)).updateCardTag(999L, 999L, tag);
	}

	@Test
	@DisplayName("Should delete card tag")
	void shouldDeleteCardTag() {
		// Given
		doNothing().when(this.cardTagService).deleteCardTag(1L, 1L);

		// When
		final ResponseEntity<Void> response = this.cardController.deleteCardTag(1, 1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(this.cardTagService, times(1)).deleteCardTag(1L, 1L);
	}
}
