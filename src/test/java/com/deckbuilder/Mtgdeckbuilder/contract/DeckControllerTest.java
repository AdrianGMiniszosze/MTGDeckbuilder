package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.Mtgdeckbuilder.application.DeckService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        testDeck = Deck.builder()
                .id(1L)
                .name("Test Deck")
                .description("A test deck")
                .userId(1L)
                .formatId(1L)
                .deckType("main")
                .isPrivate(false)
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .build();

        testDeckDTO = DeckDTO.builder()
                .id(1)
                .deck_name("Test Deck")
                .description("A test deck")
                .user_id(1)
                .format(1)
                .deck_type("main")
                .is_private(false)
                .creation_date(LocalDateTime.now())
                .last_modification(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should list all decks with default pagination")
    void shouldListAllDecks_WithDefaultPagination() {
        // Given
        Deck deck2 = testDeck.toBuilder().id(2L).name("Deck 2").build();
        DeckDTO deckDTO2 = DeckDTO.builder()
                .id(2)
                .deck_name("Deck 2")
                .user_id(1)
                .format(1)
                .build();

        when(deckService.getAll(10, 0)).thenReturn(Arrays.asList(testDeck, deck2));
        when(deckMapper.toDecksDTO(anyList())).thenReturn(Arrays.asList(testDeckDTO, deckDTO2));

        // When
        ResponseEntity<List<DeckDTO>> response = deckController.listDecks(null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getDeck_name()).isEqualTo("Test Deck");
        verify(deckService).getAll(10, 0);
    }

    @Test
    @DisplayName("Should list decks with custom pagination")
    void shouldListDecks_WithCustomPagination() {
        // Given
        when(deckService.getAll(20, 1)).thenReturn(Arrays.asList(testDeck));
        when(deckMapper.toDecksDTO(anyList())).thenReturn(Arrays.asList(testDeckDTO));

        // When
        ResponseEntity<List<DeckDTO>> response = deckController.listDecks(20, 1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(deckService).getAll(20, 1);
    }

    @Test
    @DisplayName("Should get deck by ID when it exists")
    void shouldGetDeckById_WhenExists() {
        // Given
        when(deckService.findById(1L)).thenReturn(Optional.of(testDeck));
        when(deckMapper.toDeckDTO(testDeck)).thenReturn(testDeckDTO);

        // When
        ResponseEntity<DeckDTO> response = deckController.getDeckById(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
        assertThat(response.getBody().getDeck_name()).isEqualTo("Test Deck");
        verify(deckService).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when deck not found by ID")
    void shouldReturn404_WhenDeckNotFound() {
        // Given
        when(deckService.findById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<DeckDTO> response = deckController.getDeckById(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(deckService).findById(999L);
    }

    @Test
    @DisplayName("Should create new deck and return 201")
    void shouldCreateDeck() {
        // Given
        DeckDTO newDeckDTO = DeckDTO.builder()
                .deck_name("New Deck")
                .description("A new deck")
                .user_id(1)
                .format(1)
                .build();

        Deck newDeck = testDeck.toBuilder().id(null).name("New Deck").build();
        Deck createdDeck = testDeck.toBuilder().id(2L).name("New Deck").build();
        
        DeckDTO createdDeckDTO = DeckDTO.builder()
                .id(2)
                .deck_name("New Deck")
                .description("A new deck")
                .user_id(1)
                .format(1)
                .creation_date(LocalDateTime.now())
                .last_modification(LocalDateTime.now())
                .build();

        when(deckMapper.toDeck(newDeckDTO)).thenReturn(newDeck);
        when(deckService.create(any(Deck.class))).thenReturn(createdDeck);
        when(deckMapper.toDeckDTO(createdDeck)).thenReturn(createdDeckDTO);

        // When
        ResponseEntity<DeckDTO> response = deckController.createDeck(newDeckDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(2);
        assertThat(response.getBody().getDeck_name()).isEqualTo("New Deck");
        verify(deckService).create(any(Deck.class));
    }

    @Test
    @DisplayName("Should update existing deck and return 200")
    void shouldUpdateDeck_WhenExists() {
        // Given
        DeckDTO updatedDTO = DeckDTO.builder()
                .id(1)
                .deck_name("Updated Deck")
                .description("Updated description")
                .user_id(1)
                .format(1)
                .build();

        Deck updatedDeck = testDeck.toBuilder().name("Updated Deck").build();
        Deck savedDeck = testDeck.toBuilder().name("Updated Deck").build();
        
        DeckDTO savedDTO = DeckDTO.builder()
                .id(1)
                .deck_name("Updated Deck")
                .description("Updated description")
                .user_id(1)
                .format(1)
                .last_modification(LocalDateTime.now())
                .build();

        when(deckMapper.toDeck(updatedDTO)).thenReturn(updatedDeck);
        when(deckService.update(eq(1L), any(Deck.class))).thenReturn(savedDeck);
        when(deckMapper.toDeckDTO(savedDeck)).thenReturn(savedDTO);

        // When
        ResponseEntity<DeckDTO> response = deckController.updateDeck(1, updatedDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDeck_name()).isEqualTo("Updated Deck");
        verify(deckService).update(eq(1L), any(Deck.class));
    }

    @Test
    @DisplayName("Should delete deck and return 204 when successful")
    void shouldDeleteDeck_WhenExists() {
        // Given
        when(deckService.deleteById(1L)).thenReturn(true);

        // When
        ResponseEntity<Void> response = deckController.deleteDeck(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(deckService).deleteById(1L);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent deck")
    void shouldReturn404_WhenDeletingNonExistentDeck() {
        // Given
        when(deckService.deleteById(999L)).thenReturn(false);

        // When
        ResponseEntity<Void> response = deckController.deleteDeck(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(deckService).deleteById(999L);
    }
}
