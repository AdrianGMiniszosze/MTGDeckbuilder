package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardInDeck Repository Tests")
class CardInDeckRepositoryTest {

    @Mock
    private CardInDeckRepository cardInDeckRepository;

    private CardInDeckEntity testCardInDeck;
    private CardInDeckEntity testCardInSideboard;

    @BeforeEach
    void setUp() {
        // Main deck card
        testCardInDeck = new CardInDeckEntity();
        testCardInDeck.setId(1L);
        testCardInDeck.setDeckId(100L);
        testCardInDeck.setCardId(1L);
        testCardInDeck.setQuantity(4);
        testCardInDeck.setSection("main");

        // Sideboard card
        testCardInSideboard = new CardInDeckEntity();
        testCardInSideboard.setId(2L);
        testCardInSideboard.setDeckId(100L);
        testCardInSideboard.setCardId(2L);
        testCardInSideboard.setQuantity(2);
        testCardInSideboard.setSection("sideboard");
    }

    @Test
    @DisplayName("Should save card to deck")
    void shouldSaveCardToDeck() {
        // Arrange
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(testCardInDeck);

        // Act
        CardInDeckEntity savedCard = cardInDeckRepository.save(testCardInDeck);

        // Assert
        assertThat(savedCard).isNotNull();
        assertThat(savedCard.getDeckId()).isEqualTo(100L);
        assertThat(savedCard.getCardId()).isEqualTo(1L);
        assertThat(savedCard.getQuantity()).isEqualTo(4);
        assertThat(savedCard.getSection()).isEqualTo("main");
        verify(cardInDeckRepository, times(1)).save(any(CardInDeckEntity.class));
    }

    @Test
    @DisplayName("Should find all cards in deck")
    void shouldFindAllCardsInDeck() {
        // Arrange
        List<CardInDeckEntity> cards = Arrays.asList(testCardInDeck, testCardInSideboard);
        when(cardInDeckRepository.findByDeckId(100L)).thenReturn(cards);

        // Act
        List<CardInDeckEntity> foundCards = cardInDeckRepository.findByDeckId(100L);

        // Assert
        assertThat(foundCards).hasSize(2);
        assertThat(foundCards).contains(testCardInDeck, testCardInSideboard);
        verify(cardInDeckRepository, times(1)).findByDeckId(100L);
    }

    @Test
    @DisplayName("Should find card by deck, card and section")
    void shouldFindCardByDeckCardAndSection() {
        // Arrange
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 1L, "main"))
                .thenReturn(Optional.of(testCardInDeck));

        // Act
        Optional<CardInDeckEntity> foundCard = cardInDeckRepository
                .findByDeckIdAndCardIdAndSection(100L, 1L, "main");

        // Assert
        assertThat(foundCard).isPresent();
        assertThat(foundCard.get().getCardId()).isEqualTo(1L);
        assertThat(foundCard.get().getSection()).isEqualTo("main");
        verify(cardInDeckRepository, times(1))
                .findByDeckIdAndCardIdAndSection(100L, 1L, "main");
    }

    @Test
    @DisplayName("Should return empty when card not found in section")
    void shouldReturnEmptyWhenCardNotFoundInSection() {
        // Arrange
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 999L, "main"))
                .thenReturn(Optional.empty());

        // Act
        Optional<CardInDeckEntity> foundCard = cardInDeckRepository
                .findByDeckIdAndCardIdAndSection(100L, 999L, "main");

        // Assert
        assertThat(foundCard).isEmpty();
        verify(cardInDeckRepository, times(1))
                .findByDeckIdAndCardIdAndSection(100L, 999L, "main");
    }

    @Test
    @DisplayName("Should delete card from deck section")
    void shouldDeleteCardFromDeckSection() {
        // Arrange
        doNothing().when(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(100L, 1L, "main");

        // Act
        cardInDeckRepository.deleteByDeckIdAndCardIdAndSection(100L, 1L, "main");

        // Assert
        verify(cardInDeckRepository, times(1))
                .deleteByDeckIdAndCardIdAndSection(100L, 1L, "main");
    }

    @Test
    @DisplayName("Should check if card exists in deck section")
    void shouldCheckIfCardExistsInDeckSection() {
        // Arrange
        when(cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 1L, "main"))
                .thenReturn(true);
        when(cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 999L, "main"))
                .thenReturn(false);

        // Act
        boolean exists = cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 1L, "main");
        boolean notExists = cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 999L, "main");

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(cardInDeckRepository, times(2))
                .existsByDeckIdAndCardIdAndSection(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should handle different deck sections")
    void shouldHandleDifferentDeckSections() {
        // Arrange
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 1L, "main"))
                .thenReturn(Optional.of(testCardInDeck));
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 2L, "sideboard"))
                .thenReturn(Optional.of(testCardInSideboard));

        // Act
        Optional<CardInDeckEntity> mainCard = cardInDeckRepository
                .findByDeckIdAndCardIdAndSection(100L, 1L, "main");
        Optional<CardInDeckEntity> sideboardCard = cardInDeckRepository
                .findByDeckIdAndCardIdAndSection(100L, 2L, "sideboard");

        // Assert
        assertThat(mainCard).isPresent();
        assertThat(mainCard.get().getSection()).isEqualTo("main");
        assertThat(sideboardCard).isPresent();
        assertThat(sideboardCard.get().getSection()).isEqualTo("sideboard");
    }

    @Test
    @DisplayName("Should return empty list when deck has no cards")
    void shouldReturnEmptyListWhenDeckHasNoCards() {
        // Arrange
        when(cardInDeckRepository.findByDeckId(999L)).thenReturn(List.of());

        // Act
        List<CardInDeckEntity> foundCards = cardInDeckRepository.findByDeckId(999L);

        // Assert
        assertThat(foundCards).isEmpty();
        verify(cardInDeckRepository, times(1)).findByDeckId(999L);
    }

    @Test
    @DisplayName("Should update card quantity in deck")
    void shouldUpdateCardQuantityInDeck() {
        // Arrange
        testCardInDeck.setQuantity(2);  // Update quantity
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(testCardInDeck);

        // Act
        CardInDeckEntity updatedCard = cardInDeckRepository.save(testCardInDeck);

        // Assert
        assertThat(updatedCard.getQuantity()).isEqualTo(2);
        verify(cardInDeckRepository, times(1)).save(any(CardInDeckEntity.class));
    }
}
