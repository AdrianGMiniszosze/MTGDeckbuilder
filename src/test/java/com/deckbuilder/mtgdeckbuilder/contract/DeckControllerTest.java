package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CompleteDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckCardRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckValidationDTO;
import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckValidationMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Controller Tests")
class DeckControllerTest {

	@Mock
	private DeckService deckService;

	@Mock
	private DeckMapper deckMapper;

	@Mock
	private DeckValidationMapper deckValidationMapper;

	@InjectMocks
	private DeckController deckController;

	private Deck testDeck;
    private CompleteDeckDTO testCompleteDeckDTO;

	@BeforeEach
	void setUp() {
		this.testDeck = Deck.builder().id(1L).name("Test Deck").description("A test deck").userId(1L).formatId(1L)
				.isPrivate(false).created(LocalDateTime.now()).modified(LocalDateTime.now()).build();


        this.testCompleteDeckDTO = CompleteDeckDTO.builder().id(1).deck_name("Test Deck").description("A test deck").user_id(1)
				.format(1).is_private(false).creation_date(LocalDateTime.now())
				.last_modification(LocalDateTime.now()).build();
	}

	@Test
	@DisplayName("Should list all decks with default pagination")
	void shouldListAllDecks_WithDefaultPagination() {
		// Given
		final Deck deck2 = this.testDeck.toBuilder().id(2L).name("Deck 2").build();
		final CompleteDeckDTO deckDTO2 = CompleteDeckDTO.builder().id(2).deck_name("Deck 2").user_id(1).format(1)
				.build();

		when(this.deckService.getAll(20, 0, null, null, null)).thenReturn(Arrays.asList(this.testDeck, deck2));
		when(this.deckMapper.toCompleteDecksDTO(anyList())).thenReturn(Arrays.asList(this.testCompleteDeckDTO, deckDTO2));

		// When
		final ResponseEntity<List<CompleteDeckDTO>> response = this.deckController.listDecks(null, null, null, null,
				null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().getFirst().getDeck_name()).isEqualTo("Test Deck");
		verify(this.deckService).getAll(20, 0, null, null, null);
	}

	@Test
	@DisplayName("Should list decks with custom pagination")
	void shouldListDecks_WithCustomPagination() {
		// Given
		when(this.deckService.getAll(20, 1, null, null, null)).thenReturn(Collections.singletonList(this.testDeck));
		when(this.deckMapper.toCompleteDecksDTO(anyList())).thenReturn(Collections.singletonList(this.testCompleteDeckDTO));

		// When
		final ResponseEntity<List<CompleteDeckDTO>> response = this.deckController.listDecks(20, 1, null, null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(1);
		verify(this.deckService).getAll(20, 1, null, null, null);
	}

	@Test
	@DisplayName("Should filter decks by owner, format and status")
	void shouldFilterDecks() {
		// Given
		when(this.deckService.getAll(20, 0, 5L, 2L, DeckStatus.PUBLISHED))
			.thenReturn(Collections.singletonList(this.testDeck));
		when(this.deckMapper.toCompleteDecksDTO(anyList())).thenReturn(Collections.singletonList(this.testCompleteDeckDTO));

		// When
		final ResponseEntity<List<CompleteDeckDTO>> response = this.deckController.listDecks(null, null, 5, 2,
				"published");

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.deckService).getAll(20, 0, 5L, 2L, DeckStatus.PUBLISHED);
	}

	@Test
	@DisplayName("Should accept the draft status filter case-insensitively")
	void shouldAcceptDraftStatusFilter() {
		// Given
		when(this.deckService.getAll(20, 0, null, null, DeckStatus.DRAFT))
			.thenReturn(Collections.singletonList(this.testDeck));
		when(this.deckMapper.toCompleteDecksDTO(anyList())).thenReturn(Collections.singletonList(this.testCompleteDeckDTO));

		// When
		final ResponseEntity<List<CompleteDeckDTO>> response = this.deckController.listDecks(null, null, null, null,
				"DRAFT");

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.deckService).getAll(20, 0, null, null, DeckStatus.DRAFT);
	}

	@Test
	@DisplayName("Should reject an unknown deck status filter")
	void shouldRejectUnknownStatusFilter() {
		assertThatThrownBy(() -> this.deckController.listDecks(null, null, null, null, "archived"))
			.isInstanceOf(InvalidDeckCompositionException.class)
			.hasMessageContaining("Invalid status");

		verify(this.deckService, never()).getAll(anyInt(), anyInt(), any(), any(), any());
	}

	@Test
	@DisplayName("Should get deck by ID when it exists")
	void shouldGetDeckById_WhenExists() {
		// Given
		final DeckValidationResult validation = validation();
		when(this.deckService.findById(1L)).thenReturn(Optional.of(this.testDeck));
		when(this.deckService.validate(1L)).thenReturn(validation);
		when(this.deckMapper.toCompleteDeckDTO(this.testDeck, validation)).thenReturn(this.testCompleteDeckDTO);

		// When
		final ResponseEntity<CompleteDeckDTO> response = this.deckController.getDeckById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		assertThat(response.getBody().getDeck_name()).isEqualTo("Test Deck");
		verify(this.deckService).findById(1L);
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should throw exception when deck not found by ID")
	void shouldReturn404_WhenDeckNotFound() {
		// When/Then
		assertThatThrownBy(() -> this.deckController.getDeckById(999)).isInstanceOf(DeckNotFoundException.class);

		verify(this.deckService).findById(999L);
	}

	@Test
	@DisplayName("Should create new deck and return 201")
	void shouldCreateDeck() {
		// Given
		final DeckDTO newDeckDTO = DeckDTO.builder().deck_name("New Deck").description("A new deck").user_id(1)
				.format(1).build();

		final Deck newDeck = this.testDeck.toBuilder().id(null).name("New Deck").build();
		final Deck createdDeck = this.testDeck.toBuilder().id(2L).name("New Deck").build();
		final DeckValidationResult validation = validation();

		final DeckDTO createdDeckDTO = DeckDTO.builder().id(2).deck_name("New Deck").description("A new deck")
				.user_id(1).format(1).creation_date(LocalDateTime.now())
				.last_modification(LocalDateTime.now()).build();

		when(this.deckMapper.toDeck(newDeckDTO)).thenReturn(newDeck);
		when(this.deckService.create(any(Deck.class))).thenReturn(createdDeck);
		when(this.deckService.validate(2L)).thenReturn(validation);
		when(this.deckMapper.toDeckDTO(createdDeck, validation)).thenReturn(createdDeckDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.createDeck(newDeckDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(2);
		assertThat(response.getBody().getDeck_name()).isEqualTo("New Deck");
		verify(this.deckService).create(any(Deck.class));
		verify(this.deckService).validate(2L);
	}

	@Test
	@DisplayName("Should update existing deck and return 200")
	void shouldUpdateDeck_WhenExists() {
		// Given
		final DeckDTO updatedDTO = DeckDTO.builder().id(1).deck_name("Updated Deck").description("Updated description")
				.user_id(1).format(1).build();

		final Deck updatedDeck = this.testDeck.toBuilder().name("Updated Deck").build();
		final Deck savedDeck = this.testDeck.toBuilder().name("Updated Deck").build();
		final DeckValidationResult validation = validation();

		final DeckDTO savedDTO = DeckDTO.builder().id(1).deck_name("Updated Deck").description("Updated description")
				.user_id(1).format(1).last_modification(LocalDateTime.now()).build();

		when(this.deckMapper.toDeck(updatedDTO)).thenReturn(updatedDeck);
		when(this.deckService.update(eq(1L), any(Deck.class))).thenReturn(savedDeck);
		when(this.deckService.validate(1L)).thenReturn(validation);
		when(this.deckMapper.toDeckDTO(savedDeck, validation)).thenReturn(savedDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.updateDeck(1, updatedDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDeck_name()).isEqualTo("Updated Deck");
		verify(this.deckService).update(eq(1L), any(Deck.class));
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should return the deck diagnosis")
	void shouldGetDeckValidation() {
		// Given
		final DeckValidationResult validation = DeckValidationResult.builder()
				.deckId(1L)
				.status(DeckStatus.DRAFT)
				.validationStatus(ValidationStatus.LEGAL)
				.issues(List.of())
				.build();
		final DeckValidationDTO dto = DeckValidationDTO.builder()
				.deck_id(1)
				.status(DeckValidationDTO.Status.DRAFT)
				.validation_status(DeckValidationDTO.Validation_status.LEGAL)
				.issues(List.of())
				.build();

		when(this.deckService.validate(1L)).thenReturn(validation);
		when(this.deckValidationMapper.toDto(validation)).thenReturn(dto);

		// When
		final ResponseEntity<DeckValidationDTO> response = this.deckController.getDeckValidation(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDeck_id()).isEqualTo(1);
		assertThat(response.getBody().getValidation_status()).isEqualTo(DeckValidationDTO.Validation_status.LEGAL);
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should publish a deck")
	void shouldPublishDeck() {
		// Given
		final Deck published = this.testDeck.toBuilder().status(DeckStatus.PUBLISHED).build();
		final DeckDTO publishedDTO = DeckDTO.builder().id(1).deck_name("Test Deck").user_id(1).format(1).build();

		when(this.deckService.publish(1L)).thenReturn(published);
		when(this.deckService.validate(1L)).thenReturn(validation());
		when(this.deckMapper.toDeckDTO(eq(published), any(DeckValidationResult.class))).thenReturn(publishedDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.publishDeck(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		verify(this.deckService).publish(1L);
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should return a deck to draft")
	void shouldUnpublishDeck() {
		// Given
		final Deck draft = this.testDeck.toBuilder().status(DeckStatus.DRAFT).build();
		final DeckDTO draftDTO = DeckDTO.builder().id(1).deck_name("Test Deck").user_id(1).format(1).build();

		when(this.deckService.unpublish(1L)).thenReturn(draft);
		when(this.deckService.validate(1L)).thenReturn(validation());
		when(this.deckMapper.toDeckDTO(eq(draft), any(DeckValidationResult.class))).thenReturn(draftDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.unpublishDeck(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		verify(this.deckService).unpublish(1L);
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should add a card with its diagnosis")
	void shouldAddCardToDeck() {
		// Given
		final DeckCardRequestDTO request = DeckCardRequestDTO.builder()
				.card_id(100).quantity(4).section(DeckCardRequestDTO.Section.MAIN).build();
		final Deck updated = this.testDeck.toBuilder().status(DeckStatus.DRAFT).build();
		final DeckDTO dto = DeckDTO.builder().id(1).deck_name("Test Deck").user_id(1).format(1).build();

		when(this.deckService.addCard(1L, 100L, 4, "main")).thenReturn(updated);
		when(this.deckService.validate(1L)).thenReturn(validation());
		when(this.deckMapper.toDeckDTO(eq(updated), any(DeckValidationResult.class))).thenReturn(dto);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.addCardToDeck(1, request);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		verify(this.deckService).addCard(1L, 100L, 4, "main");
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should default the card section to main")
	void shouldDefaultCardSectionToMain() {
		// Given
		final DeckCardRequestDTO request = DeckCardRequestDTO.builder().card_id(100).quantity(2).build();

		when(this.deckService.addCard(1L, 100L, 2, "main")).thenReturn(this.testDeck);
		when(this.deckService.validate(1L)).thenReturn(validation());
		when(this.deckMapper.toDeckDTO(eq(this.testDeck), any(DeckValidationResult.class)))
				.thenReturn(DeckDTO.builder().id(1).deck_name("Test Deck").user_id(1).format(1).build());

		// When
		this.deckController.addCardToDeck(1, request);

		// Then
		verify(this.deckService).addCard(1L, 100L, 2, "main");
	}

	@Test
	@DisplayName("Should remove a card with its diagnosis")
	void shouldRemoveCardFromDeck() {
		// Given
		final Deck updated = this.testDeck.toBuilder().status(DeckStatus.DRAFT).build();
		final DeckDTO dto = DeckDTO.builder().id(1).deck_name("Test Deck").user_id(1).format(1).build();

		when(this.deckService.removeCard(1L, 100L, 2, "sideboard")).thenReturn(updated);
		when(this.deckService.validate(1L)).thenReturn(validation());
		when(this.deckMapper.toDeckDTO(eq(updated), any(DeckValidationResult.class))).thenReturn(dto);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.removeCardFromDeck(1, 100, 2, "sideboard");

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.deckService).removeCard(1L, 100L, 2, "sideboard");
		verify(this.deckService).validate(1L);
	}

	@Test
	@DisplayName("Should default the removed card section to main")
	void shouldDefaultRemovedSectionToMain() {
		// Given
		when(this.deckService.removeCard(eq(1L), eq(100L), eq(1), eq("main"))).thenReturn(this.testDeck);
		when(this.deckService.validate(1L)).thenReturn(validation());
		when(this.deckMapper.toDeckDTO(eq(this.testDeck), any(DeckValidationResult.class)))
				.thenReturn(DeckDTO.builder().id(1).deck_name("Test Deck").user_id(1).format(1).build());

		// When
		this.deckController.removeCardFromDeck(1, 100, 1, null);

		// Then
		verify(this.deckService).removeCard(1L, 100L, 1, "main");
	}

	@Test
	@DisplayName("Should delete deck and return 204 when successful")
	void shouldDeleteDeck_WhenExists() {
		// Given
		when(this.deckService.deleteById(1L)).thenReturn(true);

		// When
		final ResponseEntity<Void> response = this.deckController.deleteDeck(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(this.deckService).deleteById(1L);
	}

	@Test
	@DisplayName("Should throw exception when deleting non-existent deck")
	void shouldReturn404_WhenDeletingNonExistentDeck() {
		// When/Then
		assertThatThrownBy(() -> this.deckController.deleteDeck(999)).isInstanceOf(DeckNotFoundException.class)
				.hasMessageContaining("Deck not found with id: 999");

		verify(this.deckService).deleteById(999L);
	}

	private static DeckValidationResult validation() {
		return DeckValidationResult.builder()
				.deckId(1L)
				.status(DeckStatus.DRAFT)
				.validationStatus(ValidationStatus.LEGAL)
				.issues(List.of())
				.build();
	}
}
