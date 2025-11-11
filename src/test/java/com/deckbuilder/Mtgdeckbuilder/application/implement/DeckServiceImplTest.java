package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.DeckEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
@DisplayName("Deck Service Implementation Tests")
class DeckServiceImplTest {

	@Mock
	private DeckRepository deckRepository;

	@Mock
	private DeckEntityMapper deckEntityMapper;

	@Mock
	private CardInDeckRepository cardInDeckRepository;

	@InjectMocks
	private DeckServiceImpl deckService;

	private Deck testDeck;
	private DeckEntity testDeckEntity;
	private CardInDeckEntity testCardInDeck;

	@BeforeEach
	void setUp() {
		this.testDeck = Deck.builder().id(1L).name("Test Deck").description("A test deck").userId(1L).formatId(1L)
				.deckType("main").isPrivate(false).created(LocalDateTime.now()).modified(LocalDateTime.now()).build();

		this.testDeckEntity = new DeckEntity();
		this.testDeckEntity.setId(1L);
		this.testDeckEntity.setName("Test Deck");
		this.testDeckEntity.setDescription("A test deck");
		this.testDeckEntity.setUserId(1L);
		this.testDeckEntity.setFormatId(1L);
		this.testDeckEntity.setDeckType("main");
		this.testDeckEntity.setIsPrivate(false);
		this.testDeckEntity.setCreated(LocalDateTime.now());
		this.testDeckEntity.setModified(LocalDateTime.now());

		this.testCardInDeck = CardInDeckEntity.builder().id(1L).deckId(1L).cardId(100L).quantity(2).section("main")
				.build();
	}

	@Test
	@DisplayName("Should get all decks with pagination")
	void shouldGetAllDecks() {
		// Given
		final Deck deck2 = this.testDeck.toBuilder().id(2L).name("Deck 2").build();
		final DeckEntity entity2 = new DeckEntity();
		entity2.setId(2L);
		entity2.setName("Deck 2");

		final Page<DeckEntity> page = new PageImpl<>(Arrays.asList(this.testDeckEntity, entity2));
		when(this.deckRepository.findAll(any(Pageable.class))).thenReturn(page);
		when(this.deckEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(this.testDeck, deck2));

		// When
		final List<Deck> result = this.deckService.getAll(10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should find deck by ID when it exists")
	void shouldFindDeckById_WhenExists() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Optional<Deck> result = this.deckService.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when deck not found by ID")
	void shouldReturnEmpty_WhenDeckNotFound() {
		// Given
		when(this.deckRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Deck> result = this.deckService.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.deckRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find decks by user ID with pagination")
	void shouldFindDecksByUserId() {
		// Given
		final Page<DeckEntity> page = new PageImpl<>(Collections.singletonList(this.testDeckEntity));
		when(this.deckRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
		when(this.deckEntityMapper.toModelList(anyList())).thenReturn(Collections.singletonList(this.testDeck));

		// When
		final List<Deck> result = this.deckService.findByUserId(1L, 10, 0);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getUserId()).isEqualTo(1L);
		verify(this.deckRepository).findByUserId(eq(1L), any(Pageable.class));
	}

	@Test
	@DisplayName("Should find decks by format ID")
	void shouldFindDecksByFormat() {
		// Given
		when(this.deckRepository.findByFormatId(1L)).thenReturn(Collections.singletonList(this.testDeckEntity));
		when(this.deckEntityMapper.toModelList(anyList())).thenReturn(Collections.singletonList(this.testDeck));

		// When
		final List<Deck> result = this.deckService.findByFormat(1L);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getFormatId()).isEqualTo(1L);
		verify(this.deckRepository).findByFormatId(1L);
	}

	@Test
	@DisplayName("Should create new deck")
	void shouldCreateDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.create(newDeck);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).save(any(DeckEntity.class));
	}

	@Test
	@DisplayName("Should update existing deck")
	void shouldUpdateDeck_WhenExists() {
		// Given
		final Deck updatedDeck = this.testDeck.toBuilder().name("Updated Deck").build();
		when(this.deckRepository.existsById(1L)).thenReturn(true);
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(updatedDeck);

		// When
		final Deck result = this.deckService.update(1L, updatedDeck);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Updated Deck");
		verify(this.deckRepository).existsById(1L);
		verify(this.deckRepository).save(any(DeckEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent deck")
	void shouldThrowException_WhenUpdatingNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.deckService.update(999L, this.testDeck)).isInstanceOf(DeckNotFoundException.class)
				.hasMessageContaining("Deck not found with id: 999");

		verify(this.deckRepository).existsById(999L);
		verify(this.deckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should delete deck by ID when it exists")
	void shouldDeleteDeck_WhenExists() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);
		doNothing().when(this.deckRepository).deleteById(1L);

		// When
		final boolean result = this.deckService.deleteById(1L);

		// Then
		assertThat(result).isTrue();
		verify(this.deckRepository).existsById(1L);
		verify(this.deckRepository).deleteById(1L);
	}

	@Test
	@DisplayName("Should return false when deleting non-existent deck")
	void shouldReturnFalse_WhenDeletingNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean result = this.deckService.deleteById(999L);

		// Then
		assertThat(result).isFalse();
		verify(this.deckRepository).existsById(999L);
		verify(this.deckRepository, never()).deleteById(any());
	}

	// ==================== ADD CARD TESTS ====================

	@Test
	@DisplayName("Should add new card to deck section")
	void shouldAddNewCardToDeck() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int quantity = 4;
		final String section = "main";

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.addCard(deckId, cardId, quantity, section);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(deckId);
		verify(this.deckRepository).existsById(deckId);
		verify(this.cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, section);
		verify(this.cardInDeckRepository).save(any(CardInDeckEntity.class));
	}

	@Test
	@DisplayName("Should increment quantity when adding existing card")
	void shouldIncrementQuantity_WhenAddingExistingCard() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int addQuantity = 2;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(2).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(existingCard);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.addCard(deckId, cardId, addQuantity, section);

		// Then
		assertThat(result).isNotNull();
		assertThat(existingCard.getQuantity()).isEqualTo(4); // 2 + 2
		verify(this.cardInDeckRepository).save(existingCard);
	}

	@Test
	@DisplayName("Should add card to different sections independently")
	void shouldAddCardToDifferentSections() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// Main section
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "main"))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);

		// Sideboard section
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard"))
				.thenReturn(Optional.empty());

		// When
		final Deck result1 = this.deckService.addCard(deckId, cardId, 4, "main");
		final Deck result2 = this.deckService.addCard(deckId, cardId, 2, "sideboard");

		// Then
		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		verify(this.cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, "main");
		verify(this.cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard");
		verify(this.cardInDeckRepository, times(2)).save(any(CardInDeckEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when adding card to non-existent deck")
	void shouldThrowException_WhenAddingCardToNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.deckService.addCard(999L, 100L, 4, "main"))
				.isInstanceOf(DeckNotFoundException.class).hasMessageContaining("Deck not found with id: 999");

		verify(this.deckRepository).existsById(999L);
		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should throw exception when adding card with invalid quantity")
	void shouldThrowException_WhenAddingCardWithInvalidQuantity() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then - Zero quantity
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 0, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		// When/Then - Negative quantity
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, -5, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should throw exception when adding card with invalid section")
	void shouldThrowException_WhenAddingCardWithInvalidSection() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then - Invalid section
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 4, "invalid"))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Invalid section");

		// When/Then - Null section
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 4, null))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Invalid section");

		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should accept all valid sections: main, sideboard, maybeboard")
	void shouldAcceptAllValidSections() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(eq(deckId), eq(cardId), anyString()))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);

		// When/Then - All valid sections should work
		assertThat(this.deckService.addCard(deckId, cardId, 4, "main")).isNotNull();
		assertThat(this.deckService.addCard(deckId, cardId, 2, "sideboard")).isNotNull();
		assertThat(this.deckService.addCard(deckId, cardId, 1, "maybeboard")).isNotNull();

		verify(this.cardInDeckRepository, times(3)).save(any(CardInDeckEntity.class));
	}

	// ==================== REMOVE CARD TESTS ====================

	@Test
	@DisplayName("Should decrease quantity when removing card")
	void shouldDecreaseQuantity_WhenRemovingCard() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int removeQuantity = 1;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(4).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(existingCard);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.removeCard(deckId, cardId, removeQuantity, section);

		// Then
		assertThat(result).isNotNull();
		assertThat(existingCard.getQuantity()).isEqualTo(3); // 4 - 1
		verify(this.cardInDeckRepository).save(existingCard);
		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should delete card when quantity reaches zero")
	void shouldDeleteCard_WhenQuantityReachesZero() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int removeQuantity = 4;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(4).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.removeCard(deckId, cardId, removeQuantity, section);

		// Then
		assertThat(result).isNotNull();
		verify(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should delete card when removing more than available quantity")
	void shouldDeleteCard_WhenRemovingMoreThanAvailable() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int removeQuantity = 10;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(4).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.removeCard(deckId, cardId, removeQuantity, section);

		// Then
		assertThat(result).isNotNull();
		verify(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
	}

	@Test
	@DisplayName("Should throw exception when removing card from non-existent deck")
	void shouldThrowException_WhenRemovingCardFromNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.deckService.removeCard(999L, 100L, 1, "main"))
				.isInstanceOf(DeckNotFoundException.class).hasMessageContaining("Deck not found with id: 999");

		verify(this.deckRepository).existsById(999L);
		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should throw exception when removing non-existent card from deck")
	void shouldThrowException_WhenRemovingNonExistentCard() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 999L;
		final String section = "main";

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.deckService.removeCard(deckId, cardId, 1, section))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Card not found in deck section");

		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should throw exception when removing card with invalid quantity")
	void shouldThrowException_WhenRemovingCardWithInvalidQuantity() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then - Zero quantity
		assertThatThrownBy(() -> this.deckService.removeCard(1L, 100L, 0, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		// When/Then - Negative quantity
		assertThatThrownBy(() -> this.deckService.removeCard(1L, 100L, -5, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should throw exception when removing card with invalid section")
	void shouldThrowException_WhenRemovingCardWithInvalidSection() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then
		assertThatThrownBy(() -> this.deckService.removeCard(1L, 100L, 1, "invalid"))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Invalid section");

		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should remove card from specific section only")
	void shouldRemoveCardFromSpecificSectionOnly() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;

		final CardInDeckEntity mainCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId).quantity(4)
				.section("main").build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "main"))
				.thenReturn(Optional.of(mainCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "main");
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.removeCard(deckId, cardId, 4, "main");

		// Then - Should only delete from main, not from sideboard
		verify(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "main");
		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard");
	}
}
