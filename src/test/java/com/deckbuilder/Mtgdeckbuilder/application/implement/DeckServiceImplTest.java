package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.DeckEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
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

        testCardInDeck = CardInDeckEntity.builder()
                .id(1L)
                .deckId(1L)
                .cardId(100L)
                .quantity(2)
                .section("main")
                .build();
    }

    @Test
    @DisplayName("Should get all decks with pagination")
    void shouldGetAllDecks() {
        // Given
        Deck deck2 = testDeck.toBuilder().id(2L).name("Deck 2").build();
        DeckEntity entity2 = new DeckEntity();
        entity2.setId(2L);
        entity2.setName("Deck 2");

        Page<DeckEntity> page = new PageImpl<>(Arrays.asList(testDeckEntity, entity2));
        when(deckRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(deckEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testDeck, deck2));

        // When
        List<Deck> result = deckService.getAll(10, 0);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Test Deck");
        verify(deckRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should find deck by ID when it exists")
    void shouldFindDeckById_WhenExists() {
        // Given
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Optional<Deck> result = deckService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Test Deck");
        verify(deckRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when deck not found by ID")
    void shouldReturnEmpty_WhenDeckNotFound() {
        // Given
        when(deckRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Deck> result = deckService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(deckRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find decks by user ID with pagination")
    void shouldFindDecksByUserId() {
        // Given
        Page<DeckEntity> page = new PageImpl<>(Arrays.asList(testDeckEntity));
        when(deckRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(deckEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testDeck));

        // When
        List<Deck> result = deckService.findByUserId(1L, 10, 0);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        verify(deckRepository).findByUserId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Should find decks by format ID")
    void shouldFindDecksByFormat() {
        // Given
        when(deckRepository.findByFormatId(1L)).thenReturn(Arrays.asList(testDeckEntity));
        when(deckEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testDeck));

        // When
        List<Deck> result = deckService.findByFormat(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFormatId()).isEqualTo(1L);
        verify(deckRepository).findByFormatId(1L);
    }

    @Test
    @DisplayName("Should create new deck")
    void shouldCreateDeck() {
        // Given
        Deck newDeck = testDeck.toBuilder().id(null).created(null).modified(null).build();
        when(deckEntityMapper.toEntity(any(Deck.class))).thenReturn(testDeckEntity);
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(testDeckEntity);
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Deck result = deckService.create(newDeck);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Deck");
        verify(deckRepository).save(any(DeckEntity.class));
    }

    @Test
    @DisplayName("Should update existing deck")
    void shouldUpdateDeck_WhenExists() {
        // Given
        Deck updatedDeck = testDeck.toBuilder().name("Updated Deck").build();
        when(deckRepository.existsById(1L)).thenReturn(true);
        when(deckEntityMapper.toEntity(any(Deck.class))).thenReturn(testDeckEntity);
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(testDeckEntity);
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(updatedDeck);

        // When
        Deck result = deckService.update(1L, updatedDeck);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Deck");
        verify(deckRepository).existsById(1L);
        verify(deckRepository).save(any(DeckEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent deck")
    void shouldThrowException_WhenUpdatingNonExistentDeck() {
        // Given
        when(deckRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> deckService.update(999L, testDeck))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deck not found with id: 999");
        
        verify(deckRepository).existsById(999L);
        verify(deckRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete deck by ID when it exists")
    void shouldDeleteDeck_WhenExists() {
        // Given
        when(deckRepository.existsById(1L)).thenReturn(true);
        doNothing().when(deckRepository).deleteById(1L);

        // When
        boolean result = deckService.deleteById(1L);

        // Then
        assertThat(result).isTrue();
        verify(deckRepository).existsById(1L);
        verify(deckRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent deck")
    void shouldReturnFalse_WhenDeletingNonExistentDeck() {
        // Given
        when(deckRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = deckService.deleteById(999L);

        // Then
        assertThat(result).isFalse();
        verify(deckRepository).existsById(999L);
        verify(deckRepository, never()).deleteById(any());
    }

    // ==================== ADD CARD TESTS ====================

    @Test
    @DisplayName("Should add new card to deck section")
    void shouldAddNewCardToDeck() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;
        int quantity = 4;
        String section = "main";

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
                .thenReturn(Optional.empty());
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(testCardInDeck);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Deck result = deckService.addCard(deckId, cardId, quantity, section);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(deckId);
        verify(deckRepository).existsById(deckId);
        verify(cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, section);
        verify(cardInDeckRepository).save(any(CardInDeckEntity.class));
    }

    @Test
    @DisplayName("Should increment quantity when adding existing card")
    void shouldIncrementQuantity_WhenAddingExistingCard() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;
        int addQuantity = 2;
        String section = "main";

        CardInDeckEntity existingCard = CardInDeckEntity.builder()
                .id(1L)
                .deckId(deckId)
                .cardId(cardId)
                .quantity(2)
                .section(section)
                .build();

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
                .thenReturn(Optional.of(existingCard));
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(existingCard);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Deck result = deckService.addCard(deckId, cardId, addQuantity, section);

        // Then
        assertThat(result).isNotNull();
        assertThat(existingCard.getQuantity()).isEqualTo(4); // 2 + 2
        verify(cardInDeckRepository).save(existingCard);
    }

    @Test
    @DisplayName("Should add card to different sections independently")
    void shouldAddCardToDifferentSections() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // Main section
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "main"))
                .thenReturn(Optional.empty());
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(testCardInDeck);

        // Sideboard section
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard"))
                .thenReturn(Optional.empty());

        // When
        Deck result1 = deckService.addCard(deckId, cardId, 4, "main");
        Deck result2 = deckService.addCard(deckId, cardId, 2, "sideboard");

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        verify(cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, "main");
        verify(cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard");
        verify(cardInDeckRepository, times(2)).save(any(CardInDeckEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when adding card to non-existent deck")
    void shouldThrowException_WhenAddingCardToNonExistentDeck() {
        // Given
        when(deckRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> deckService.addCard(999L, 100L, 4, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deck not found with id: 999");

        verify(deckRepository).existsById(999L);
        verify(cardInDeckRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when adding card with invalid quantity")
    void shouldThrowException_WhenAddingCardWithInvalidQuantity() {
        // Given
        when(deckRepository.existsById(1L)).thenReturn(true);

        // When/Then - Zero quantity
        assertThatThrownBy(() -> deckService.addCard(1L, 100L, 0, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than 0");

        // When/Then - Negative quantity
        assertThatThrownBy(() -> deckService.addCard(1L, 100L, -5, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than 0");

        verify(cardInDeckRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when adding card with invalid section")
    void shouldThrowException_WhenAddingCardWithInvalidSection() {
        // Given
        when(deckRepository.existsById(1L)).thenReturn(true);

        // When/Then - Invalid section
        assertThatThrownBy(() -> deckService.addCard(1L, 100L, 4, "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid section");

        // When/Then - Null section
        assertThatThrownBy(() -> deckService.addCard(1L, 100L, 4, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid section");

        verify(cardInDeckRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should accept all valid sections: main, sideboard, maybeboard")
    void shouldAcceptAllValidSections() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(eq(deckId), eq(cardId), anyString()))
                .thenReturn(Optional.empty());
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(testCardInDeck);

        // When/Then - All valid sections should work
        assertThat(deckService.addCard(deckId, cardId, 4, "main")).isNotNull();
        assertThat(deckService.addCard(deckId, cardId, 2, "sideboard")).isNotNull();
        assertThat(deckService.addCard(deckId, cardId, 1, "maybeboard")).isNotNull();

        verify(cardInDeckRepository, times(3)).save(any(CardInDeckEntity.class));
    }

    // ==================== REMOVE CARD TESTS ====================

    @Test
    @DisplayName("Should decrease quantity when removing card")
    void shouldDecreaseQuantity_WhenRemovingCard() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;
        int removeQuantity = 1;
        String section = "main";

        CardInDeckEntity existingCard = CardInDeckEntity.builder()
                .id(1L)
                .deckId(deckId)
                .cardId(cardId)
                .quantity(4)
                .section(section)
                .build();

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
                .thenReturn(Optional.of(existingCard));
        when(cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(existingCard);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Deck result = deckService.removeCard(deckId, cardId, removeQuantity, section);

        // Then
        assertThat(result).isNotNull();
        assertThat(existingCard.getQuantity()).isEqualTo(3); // 4 - 1
        verify(cardInDeckRepository).save(existingCard);
        verify(cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
    }

    @Test
    @DisplayName("Should delete card when quantity reaches zero")
    void shouldDeleteCard_WhenQuantityReachesZero() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;
        int removeQuantity = 4;
        String section = "main";

        CardInDeckEntity existingCard = CardInDeckEntity.builder()
                .id(1L)
                .deckId(deckId)
                .cardId(cardId)
                .quantity(4)
                .section(section)
                .build();

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
                .thenReturn(Optional.of(existingCard));
        doNothing().when(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Deck result = deckService.removeCard(deckId, cardId, removeQuantity, section);

        // Then
        assertThat(result).isNotNull();
        verify(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
        verify(cardInDeckRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete card when removing more than available quantity")
    void shouldDeleteCard_WhenRemovingMoreThanAvailable() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;
        int removeQuantity = 10;
        String section = "main";

        CardInDeckEntity existingCard = CardInDeckEntity.builder()
                .id(1L)
                .deckId(deckId)
                .cardId(cardId)
                .quantity(4)
                .section(section)
                .build();

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
                .thenReturn(Optional.of(existingCard));
        doNothing().when(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        Deck result = deckService.removeCard(deckId, cardId, removeQuantity, section);

        // Then
        assertThat(result).isNotNull();
        verify(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
    }

    @Test
    @DisplayName("Should throw exception when removing card from non-existent deck")
    void shouldThrowException_WhenRemovingCardFromNonExistentDeck() {
        // Given
        when(deckRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> deckService.removeCard(999L, 100L, 1, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deck not found with id: 999");

        verify(deckRepository).existsById(999L);
        verify(cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent card from deck")
    void shouldThrowException_WhenRemovingNonExistentCard() {
        // Given
        Long deckId = 1L;
        Long cardId = 999L;
        String section = "main";

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deckService.removeCard(deckId, cardId, 1, section))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card not found in deck section");

        verify(cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when removing card with invalid quantity")
    void shouldThrowException_WhenRemovingCardWithInvalidQuantity() {
        // Given
        when(deckRepository.existsById(1L)).thenReturn(true);

        // When/Then - Zero quantity
        assertThatThrownBy(() -> deckService.removeCard(1L, 100L, 0, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than 0");

        // When/Then - Negative quantity
        assertThatThrownBy(() -> deckService.removeCard(1L, 100L, -5, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than 0");

        verify(cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when removing card with invalid section")
    void shouldThrowException_WhenRemovingCardWithInvalidSection() {
        // Given
        when(deckRepository.existsById(1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> deckService.removeCard(1L, 100L, 1, "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid section");

        verify(cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
    }

    @Test
    @DisplayName("Should remove card from specific section only")
    void shouldRemoveCardFromSpecificSectionOnly() {
        // Given
        Long deckId = 1L;
        Long cardId = 100L;

        CardInDeckEntity mainCard = CardInDeckEntity.builder()
                .id(1L)
                .deckId(deckId)
                .cardId(cardId)
                .quantity(4)
                .section("main")
                .build();

        when(deckRepository.existsById(deckId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "main"))
                .thenReturn(Optional.of(mainCard));
        doNothing().when(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "main");
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeckEntity));
        when(deckEntityMapper.toModel(testDeckEntity)).thenReturn(testDeck);

        // When
        deckService.removeCard(deckId, cardId, 4, "main");

        // Then - Should only delete from main, not from sideboard
        verify(cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "main");
        verify(cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard");
    }
}
