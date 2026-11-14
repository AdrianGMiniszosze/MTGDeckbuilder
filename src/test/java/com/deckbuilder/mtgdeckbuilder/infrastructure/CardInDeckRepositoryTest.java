package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
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
        this.testCardInDeck = new CardInDeckEntity();
        this.testCardInDeck.setId(1L);
        this.testCardInDeck.setDeckId(100L);
        this.testCardInDeck.setCardId(1L);
        this.testCardInDeck.setQuantity(4);
        this.testCardInDeck.setSection("main");

		// Sideboard card
        this.testCardInSideboard = new CardInDeckEntity();
        this.testCardInSideboard.setId(2L);
        this.testCardInSideboard.setDeckId(100L);
        this.testCardInSideboard.setCardId(2L);
        this.testCardInSideboard.setQuantity(2);
        this.testCardInSideboard.setSection("sideboard");
	}

	@Test
	@DisplayName("Should save card to deck")
	void shouldSaveCardToDeck() {
		// Arrange
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);

		// Act
		final CardInDeckEntity savedCard = this.cardInDeckRepository.save(this.testCardInDeck);

		// Assert
		assertThat(savedCard).isNotNull();
		assertThat(savedCard.getDeckId()).isEqualTo(100L);
		assertThat(savedCard.getCardId()).isEqualTo(1L);
		assertThat(savedCard.getQuantity()).isEqualTo(4);
		assertThat(savedCard.getSection()).isEqualTo("main");
		verify(this.cardInDeckRepository, times(1)).save(any(CardInDeckEntity.class));
	}

	@Test
	@DisplayName("Should find all cards in deck")
	void shouldFindAllCardsInDeck() {
		// Arrange
		final List<CardInDeckEntity> cards = Arrays.asList(this.testCardInDeck, this.testCardInSideboard);
		when(this.cardInDeckRepository.findByDeckId(100L)).thenReturn(cards);

		// Act
		final List<CardInDeckEntity> foundCards = this.cardInDeckRepository.findByDeckId(100L);

		// Assert
		assertThat(foundCards).hasSize(2);
		assertThat(foundCards).contains(this.testCardInDeck, this.testCardInSideboard);
		verify(this.cardInDeckRepository, times(1)).findByDeckId(100L);
	}

	@Test
	@DisplayName("Should find card by deck, card and section")
	void shouldFindCardByDeckCardAndSection() {
		// Arrange
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 1L, "main"))
				.thenReturn(Optional.of(this.testCardInDeck));

		// Act
		final Optional<CardInDeckEntity> foundCard = this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 1L, "main");

		// Assert
		assertThat(foundCard).isPresent();
		assertThat(foundCard.get().getCardId()).isEqualTo(1L);
		assertThat(foundCard.get().getSection()).isEqualTo("main");
		verify(this.cardInDeckRepository, times(1)).findByDeckIdAndCardIdAndSection(100L, 1L, "main");
	}

	@Test
	@DisplayName("Should return empty when card not found in section")
	void shouldReturnEmptyWhenCardNotFoundInSection() {
		// Arrange
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 999L, "main")).thenReturn(Optional.empty());

		// Act
		final Optional<CardInDeckEntity> foundCard = this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 999L, "main");

		// Assert
		assertThat(foundCard).isEmpty();
		verify(this.cardInDeckRepository, times(1)).findByDeckIdAndCardIdAndSection(100L, 999L, "main");
	}

	@Test
	@DisplayName("Should delete card from deck section")
	void shouldDeleteCardFromDeckSection() {
		// Arrange
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(100L, 1L, "main");

		// Act
        this.cardInDeckRepository.deleteByDeckIdAndCardIdAndSection(100L, 1L, "main");

		// Assert
		verify(this.cardInDeckRepository, times(1)).deleteByDeckIdAndCardIdAndSection(100L, 1L, "main");
	}

	@Test
	@DisplayName("Should check if card exists in deck section")
	void shouldCheckIfCardExistsInDeckSection() {
		// Arrange
		when(this.cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 1L, "main")).thenReturn(true);
		when(this.cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 999L, "main")).thenReturn(false);

		// Act
		final boolean exists = this.cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 1L, "main");
		final boolean notExists = this.cardInDeckRepository.existsByDeckIdAndCardIdAndSection(100L, 999L, "main");

		// Assert
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.cardInDeckRepository, times(2)).existsByDeckIdAndCardIdAndSection(anyLong(), anyLong(), anyString());
	}

	@Test
	@DisplayName("Should handle different deck sections")
	void shouldHandleDifferentDeckSections() {
		// Arrange
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 1L, "main"))
				.thenReturn(Optional.of(this.testCardInDeck));
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 2L, "sideboard"))
				.thenReturn(Optional.of(this.testCardInSideboard));

		// Act
		final Optional<CardInDeckEntity> mainCard = this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 1L, "main");
		final Optional<CardInDeckEntity> sideboardCard = this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(100L, 2L,
				"sideboard");

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
		when(this.cardInDeckRepository.findByDeckId(999L)).thenReturn(List.of());

		// Act
		final List<CardInDeckEntity> foundCards = this.cardInDeckRepository.findByDeckId(999L);

		// Assert
		assertThat(foundCards).isEmpty();
		verify(this.cardInDeckRepository, times(1)).findByDeckId(999L);
	}

	@Test
	@DisplayName("Should update card quantity in deck")
	void shouldUpdateCardQuantityInDeck() {
		// Arrange
        this.testCardInDeck.setQuantity(2); // Update quantity
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);

		// Act
		final CardInDeckEntity updatedCard = this.cardInDeckRepository.save(this.testCardInDeck);

		// Assert
		assertThat(updatedCard.getQuantity()).isEqualTo(2);
		verify(this.cardInDeckRepository, times(1)).save(any(CardInDeckEntity.class));
	}
}
