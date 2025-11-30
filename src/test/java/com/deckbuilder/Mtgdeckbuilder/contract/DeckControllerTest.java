package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Controller Tests")
class DeckControllerTest {

	@Mock
	private DeckService deckService;

	@Mock
	private DeckMapper deckMapper;

	@InjectMocks
	private DeckController deckController;

	private Deck testDeck;
	private DeckDTO testDeckDTO;

	@BeforeEach
	void setUp() {
		this.testDeck = Deck.builder().id(1L).name("Test Deck").description("A test deck").userId(1L).formatId(1L)
				.deckType("main").isPrivate(false).created(LocalDateTime.now()).modified(LocalDateTime.now()).build();

		this.testDeckDTO = DeckDTO.builder().id(1).deck_name("Test Deck").description("A test deck").user_id(1)
				.format(1).deck_type(DeckDTO.Deck_type.MAIN).is_private(false).creation_date(LocalDateTime.now())
				.last_modification(LocalDateTime.now()).build();
	}

	@Test
	@DisplayName("Should list all decks with default pagination")
	void shouldListAllDecks_WithDefaultPagination() {
		// Given
		final Deck deck2 = this.testDeck.toBuilder().id(2L).name("Deck 2").build();
		final DeckDTO deckDTO2 = DeckDTO.builder().id(2).deck_name("Deck 2").user_id(1).format(1)
				.deck_type(DeckDTO.Deck_type.MAIN).build();

		when(this.deckService.getAll(10, 0)).thenReturn(Arrays.asList(this.testDeck, deck2));
		when(this.deckMapper.toDecksDTO(anyList())).thenReturn(Arrays.asList(this.testDeckDTO, deckDTO2));

		// When
		final ResponseEntity<List<DeckDTO>> response = this.deckController.listDecks(null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getDeck_name()).isEqualTo("Test Deck");
		verify(this.deckService).getAll(10, 0);
	}

	@Test
	@DisplayName("Should list decks with custom pagination")
	void shouldListDecks_WithCustomPagination() {
		// Given
		when(this.deckService.getAll(20, 1)).thenReturn(Collections.singletonList(this.testDeck));
		when(this.deckMapper.toDecksDTO(anyList())).thenReturn(Collections.singletonList(this.testDeckDTO));

		// When
		final ResponseEntity<List<DeckDTO>> response = this.deckController.listDecks(20, 1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(1);
		verify(this.deckService).getAll(20, 1);
	}

	@Test
	@DisplayName("Should get deck by ID when it exists")
	void shouldGetDeckById_WhenExists() {
		// Given
		when(this.deckService.findById(1L)).thenReturn(Optional.of(this.testDeck));
		when(this.deckMapper.toDeckDTO(this.testDeck)).thenReturn(this.testDeckDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.getDeckById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		assertThat(response.getBody().getDeck_name()).isEqualTo("Test Deck");
		verify(this.deckService).findById(1L);
	}

	@Test
	@DisplayName("Should throw exception when deck not found by ID")
	void shouldReturn404_WhenDeckNotFound() {
		// When/Then
		assertThatThrownBy(() -> this.deckController.getDeckById(999)).isInstanceOf(DeckNotFoundException.class)
				.hasMessageContaining("Deck not found with id: 999");

		verify(this.deckService).findById(999L);
	}

	@Test
	@DisplayName("Should create new deck and return 201")
	void shouldCreateDeck() {
		// Given
		final DeckDTO newDeckDTO = DeckDTO.builder().deck_name("New Deck").description("A new deck").user_id(1)
				.format(1).deck_type(DeckDTO.Deck_type.MAIN).build();

		final Deck newDeck = this.testDeck.toBuilder().id(null).name("New Deck").build();
		final Deck createdDeck = this.testDeck.toBuilder().id(2L).name("New Deck").build();

		final DeckDTO createdDeckDTO = DeckDTO.builder().id(2).deck_name("New Deck").description("A new deck")
				.user_id(1).format(1).deck_type(DeckDTO.Deck_type.MAIN).creation_date(LocalDateTime.now())
				.last_modification(LocalDateTime.now()).build();

		when(this.deckMapper.toDeck(newDeckDTO)).thenReturn(newDeck);
		when(this.deckService.create(any(Deck.class))).thenReturn(createdDeck);
		when(this.deckMapper.toDeckDTO(createdDeck)).thenReturn(createdDeckDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.createDeck(newDeckDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(2);
		assertThat(response.getBody().getDeck_name()).isEqualTo("New Deck");
		verify(this.deckService).create(any(Deck.class));
	}

	@Test
	@DisplayName("Should update existing deck and return 200")
	void shouldUpdateDeck_WhenExists() {
		// Given
		final DeckDTO updatedDTO = DeckDTO.builder().id(1).deck_name("Updated Deck").description("Updated description")
				.user_id(1).format(1).deck_type(DeckDTO.Deck_type.MAIN).build();

		final Deck updatedDeck = this.testDeck.toBuilder().name("Updated Deck").build();
		final Deck savedDeck = this.testDeck.toBuilder().name("Updated Deck").build();

		final DeckDTO savedDTO = DeckDTO.builder().id(1).deck_name("Updated Deck").description("Updated description")
				.user_id(1).format(1).deck_type(DeckDTO.Deck_type.MAIN).last_modification(LocalDateTime.now()).build();

		when(this.deckMapper.toDeck(updatedDTO)).thenReturn(updatedDeck);
		when(this.deckService.update(eq(1L), any(Deck.class))).thenReturn(savedDeck);
		when(this.deckMapper.toDeckDTO(savedDeck)).thenReturn(savedDTO);

		// When
		final ResponseEntity<DeckDTO> response = this.deckController.updateDeck(1, updatedDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDeck_name()).isEqualTo("Updated Deck");
		verify(this.deckService).update(eq(1L), any(Deck.class));
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
}
