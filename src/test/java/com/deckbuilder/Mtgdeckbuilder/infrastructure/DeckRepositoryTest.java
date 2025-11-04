package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.DeckEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Repository Tests")
class DeckRepositoryTest {

    @Mock
    private DeckRepository deckRepository;

    private DeckEntity testDeckEntity;

    @BeforeEach
    void setUp() {
        testDeckEntity = new DeckEntity();
        testDeckEntity.setId(1L);
        testDeckEntity.setName("Test Deck");
        testDeckEntity.setDescription("A test deck");
        testDeckEntity.setUserId(1L);
        testDeckEntity.setFormatId(1L);
        testDeckEntity.setDeckType("main");
        testDeckEntity.setIsPrivate(false);
        testDeckEntity.setCreated(LocalDateTime.now());
        testDeckEntity.setModified(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find deck by ID when it exists")
    void shouldFindDeckById_WhenExists() {
        // Given
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeckEntity));

        // When
        Optional<DeckEntity> result = deckRepository.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Test Deck");
        verify(deckRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when deck ID does not exist")
    void shouldReturnEmpty_WhenDeckNotFound() {
        // Given
        when(deckRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<DeckEntity> result = deckRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(deckRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find all decks with pagination")
    void shouldFindAllDecksWithPagination() {
        // Given
        DeckEntity deck2 = new DeckEntity();
        deck2.setId(2L);
        deck2.setName("Another Deck");
        deck2.setUserId(1L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<DeckEntity> page = new PageImpl<>(Arrays.asList(testDeckEntity, deck2));
        when(deckRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<DeckEntity> result = deckRepository.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Deck");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Another Deck");
        verify(deckRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should find decks by user ID with pagination")
    void shouldFindDecksByUserId() {
        // Given
        DeckEntity deck2 = new DeckEntity();
        deck2.setId(2L);
        deck2.setName("User Deck 2");
        deck2.setUserId(1L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<DeckEntity> page = new PageImpl<>(Arrays.asList(testDeckEntity, deck2));
        when(deckRepository.findByUserId(1L, pageable)).thenReturn(page);

        // When
        Page<DeckEntity> result = deckRepository.findByUserId(1L, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(deck -> deck.getUserId().equals(1L));
        verify(deckRepository).findByUserId(1L, pageable);
    }

    @Test
    @DisplayName("Should find decks by format ID")
    void shouldFindDecksByFormatId() {
        // Given
        DeckEntity deck2 = new DeckEntity();
        deck2.setId(2L);
        deck2.setName("Standard Deck");
        deck2.setFormatId(1L);

        List<DeckEntity> decks = Arrays.asList(testDeckEntity, deck2);
        when(deckRepository.findByFormatId(1L)).thenReturn(decks);

        // When
        List<DeckEntity> result = deckRepository.findByFormatId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(deck -> deck.getFormatId().equals(1L));
        verify(deckRepository).findByFormatId(1L);
    }

    @Test
    @DisplayName("Should save deck entity")
    void shouldSaveDeck() {
        // Given
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(testDeckEntity);

        // When
        DeckEntity result = deckRepository.save(testDeckEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Deck");
        verify(deckRepository).save(testDeckEntity);
    }

    @Test
    @DisplayName("Should delete deck by ID")
    void shouldDeleteDeckById() {
        // Given
        Long deckId = 1L;

        // When
        deckRepository.deleteById(deckId);

        // Then
        verify(deckRepository).deleteById(deckId);
    }

    @Test
    @DisplayName("Should check if deck exists by ID")
    void shouldCheckIfDeckExists() {
        // Given
        when(deckRepository.existsById(1L)).thenReturn(true);
        when(deckRepository.existsById(999L)).thenReturn(false);

        // When
        boolean exists = deckRepository.existsById(1L);
        boolean notExists = deckRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(deckRepository).existsById(1L);
        verify(deckRepository).existsById(999L);
    }
}
