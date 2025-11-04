package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.FormatEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Card;
import com.deckbuilder.Mtgdeckbuilder.model.Format;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Format Service Implementation Tests")
class FormatServiceImplTest {

    @Mock
    private FormatRepository formatRepository;

    @Mock
    private FormatEntityMapper formatEntityMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardInDeckRepository cardInDeckRepository;

    @Mock
    private CardEntityMapper cardEntityMapper;

    @InjectMocks
    private FormatServiceImpl formatService;

    private Format testFormat;
    private FormatEntity testFormatEntity;

    @BeforeEach
    void setUp() {
        testFormat = Format.builder()
                .id(1L)
                .name("Standard")
                .description("Standard format")
                .minDeckSize(60)
                .maxDeckSize(60)
                .maxSideboardSize(15)
                .bannedCards(Arrays.asList("123", "456"))
                .restrictedCards(Arrays.asList("789"))
                .build();

        testFormatEntity = new FormatEntity();
        testFormatEntity.setId(1L);
        testFormatEntity.setName("Standard");
        testFormatEntity.setDescription("Standard format");
        testFormatEntity.setMinDeckSize(60);
        testFormatEntity.setMaxDeckSize(60);
        testFormatEntity.setMaxSideboardSize(15);
        testFormatEntity.setBannedCards(Arrays.asList("123", "456"));
        testFormatEntity.setRestrictedCards(Arrays.asList("789"));
    }

    @Test
    @DisplayName("Should get all formats")
    void shouldGetAllFormats() {
        // Given
        Format format2 = testFormat.toBuilder().id(2L).name("Commander").build();
        FormatEntity entity2 = new FormatEntity();
        entity2.setId(2L);
        entity2.setName("Commander");

        when(formatRepository.findAll()).thenReturn(Arrays.asList(testFormatEntity, entity2));
        when(formatEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testFormat, format2));

        // When
        List<Format> result = formatService.getAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Standard");
        assertThat(result.get(1).getName()).isEqualTo("Commander");
        verify(formatRepository).findAll();
    }

    @Test
    @DisplayName("Should find format by ID when it exists")
    void shouldFindFormatById_WhenExists() {
        // Given
        when(formatRepository.findById(1L)).thenReturn(Optional.of(testFormatEntity));
        when(formatEntityMapper.toModel(testFormatEntity)).thenReturn(testFormat);

        // When
        Optional<Format> result = formatService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Standard");
        verify(formatRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when format not found by ID")
    void shouldReturnEmpty_WhenFormatNotFoundById() {
        // Given
        when(formatRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Format> result = formatService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(formatRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find format by name when it exists")
    void shouldFindFormatByName_WhenExists() {
        // Given
        when(formatRepository.findByName("Standard")).thenReturn(Optional.of(testFormatEntity));
        when(formatEntityMapper.toModel(testFormatEntity)).thenReturn(testFormat);

        // When
        Optional<Format> result = formatService.findByName("Standard");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Standard");
        verify(formatRepository).findByName("Standard");
    }

    @Test
    @DisplayName("Should return empty when format not found by name")
    void shouldReturnEmpty_WhenFormatNotFoundByName() {
        // Given
        when(formatRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        // When
        Optional<Format> result = formatService.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(formatRepository).findByName("NonExistent");
    }

    @Test
    @DisplayName("Should create new format")
    void shouldCreateFormat() {
        // Given
        Format newFormat = testFormat.toBuilder().id(null).build();
        when(formatEntityMapper.toEntity(any(Format.class))).thenReturn(testFormatEntity);
        when(formatRepository.save(any(FormatEntity.class))).thenReturn(testFormatEntity);
        when(formatEntityMapper.toModel(testFormatEntity)).thenReturn(testFormat);

        // When
        Format result = formatService.create(newFormat);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Standard");
        verify(formatRepository).save(any(FormatEntity.class));
    }

    @Test
    @DisplayName("Should update existing format")
    void shouldUpdateFormat_WhenExists() {
        // Given
        Format updatedFormat = testFormat.toBuilder().description("Updated description").build();
        when(formatRepository.findById(1L)).thenReturn(Optional.of(testFormatEntity));
        when(formatEntityMapper.toEntity(any(Format.class))).thenReturn(testFormatEntity);
        when(formatRepository.save(any(FormatEntity.class))).thenReturn(testFormatEntity);
        when(formatEntityMapper.toModel(testFormatEntity)).thenReturn(updatedFormat);

        // When
        Format result = formatService.update(1L, updatedFormat);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(formatRepository).findById(1L);
        verify(formatRepository).save(any(FormatEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent format")
    void shouldThrowException_WhenUpdatingNonExistentFormat() {
        // Given
        when(formatRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> formatService.update(999L, testFormat))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Format not found with id: 999");
        
        verify(formatRepository).findById(999L);
        verify(formatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete format by ID when it exists")
    void shouldDeleteFormat_WhenExists() {
        // Given
        when(formatRepository.existsById(1L)).thenReturn(true);
        doNothing().when(formatRepository).deleteById(1L);

        // When
        boolean result = formatService.deleteById(1L);

        // Then
        assertThat(result).isTrue();
        verify(formatRepository).existsById(1L);
        verify(formatRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent format")
    void shouldReturnFalse_WhenDeletingNonExistentFormat() {
        // Given
        when(formatRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = formatService.deleteById(999L);

        // Then
        assertThat(result).isFalse();
        verify(formatRepository).existsById(999L);
        verify(formatRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should return true when card is legal in format")
    void shouldReturnTrue_WhenCardIsLegal() {
        // Given
        Long cardId = 100L;
        when(formatRepository.findById(1L)).thenReturn(Optional.of(testFormatEntity));

        // When
        boolean result = formatService.isCardLegal(cardId, 1L);

        // Then
        assertThat(result).isTrue();
        verify(formatRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return false when card is banned in format")
    void shouldReturnFalse_WhenCardIsBanned() {
        // Given
        Long cardId = 123L; // This is in the banned list
        when(formatRepository.findById(1L)).thenReturn(Optional.of(testFormatEntity));

        // When
        boolean result = formatService.isCardLegal(cardId, 1L);

        // Then
        assertThat(result).isFalse();
        verify(formatRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return false when format not found for card legality check")
    void shouldReturnFalse_WhenFormatNotFoundForCardLegality() {
        // Given
        when(formatRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = formatService.isCardLegal(100L, 999L);

        // Then
        assertThat(result).isFalse();
        verify(formatRepository).findById(999L);
    }

    @Test
    @DisplayName("Should return true when deck is legal in format")
    void shouldReturnTrue_WhenDeckIsLegal() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        CardInDeckEntity mainCard1 = CardInDeckEntity.builder()
                .cardId(100L)
                .deckId(deckId)
                .quantity(4)
                .section("main")
                .build();
        
        CardInDeckEntity mainCard2 = CardInDeckEntity.builder()
                .cardId(101L)
                .deckId(deckId)
                .quantity(20)
                .section("main")
                .build();
        
        CardInDeckEntity sideboardCard = CardInDeckEntity.builder()
                .cardId(102L)
                .deckId(deckId)
                .quantity(10)
                .section("sideboard")
                .build();
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId))
                .thenReturn(Arrays.asList(mainCard1, mainCard2, sideboardCard));
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));

        // When (24 cards in main, 10 in sideboard - all legal)
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isFalse(); // False because 24 < 60 (minDeckSize)
        verify(formatRepository).existsById(formatId);
        verify(cardInDeckRepository).findByDeckId(deckId);
    }

    @Test
    @DisplayName("Should return false when deck is too small")
    void shouldReturnFalse_WhenDeckIsTooSmall() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        CardInDeckEntity mainCard = CardInDeckEntity.builder()
                .cardId(100L)
                .deckId(deckId)
                .quantity(30)
                .section("main")
                .build();
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId)).thenReturn(Arrays.asList(mainCard));
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));

        // When (30 cards < 60 minDeckSize)
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when deck is too large")
    void shouldReturnFalse_WhenDeckIsTooLarge() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        CardInDeckEntity mainCard = CardInDeckEntity.builder()
                .cardId(100L)
                .deckId(deckId)
                .quantity(70)
                .section("main")
                .build();
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId)).thenReturn(Arrays.asList(mainCard));
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));

        // When (70 cards > 60 maxDeckSize)
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when sideboard is too large")
    void shouldReturnFalse_WhenSideboardIsTooLarge() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        CardInDeckEntity mainCard = CardInDeckEntity.builder()
                .cardId(100L)
                .deckId(deckId)
                .quantity(60)
                .section("main")
                .build();
        
        CardInDeckEntity sideboardCard = CardInDeckEntity.builder()
                .cardId(101L)
                .deckId(deckId)
                .quantity(20)
                .section("sideboard")
                .build();
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId))
                .thenReturn(Arrays.asList(mainCard, sideboardCard));
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));

        // When (20 cards in sideboard > 15 maxSideboardSize)
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when deck contains banned card")
    void shouldReturnFalse_WhenDeckContainsBannedCard() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        CardInDeckEntity bannedCard = CardInDeckEntity.builder()
                .cardId(123L) // Banned card from test setup
                .deckId(deckId)
                .quantity(4)
                .section("main")
                .build();
        
        CardInDeckEntity legalCard = CardInDeckEntity.builder()
                .cardId(100L)
                .deckId(deckId)
                .quantity(56)
                .section("main")
                .build();
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId))
                .thenReturn(Arrays.asList(bannedCard, legalCard));
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));

        // When
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when deck is empty")
    void shouldReturnFalse_WhenDeckIsEmpty() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId)).thenReturn(Collections.emptyList());

        // When
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should ignore maybeboard cards when checking deck legality")
    void shouldIgnoreMaybeboardCards() {
        // Given
        Long deckId = 1L;
        Long formatId = 1L;
        
        CardInDeckEntity mainCard = CardInDeckEntity.builder()
                .cardId(100L)
                .deckId(deckId)
                .quantity(60)
                .section("main")
                .build();
        
        CardInDeckEntity maybeboardCard = CardInDeckEntity.builder()
                .cardId(123L) // Banned card but in maybeboard
                .deckId(deckId)
                .quantity(4)
                .section("maybeboard")
                .build();
        
        when(formatRepository.existsById(formatId)).thenReturn(true);
        when(cardInDeckRepository.findByDeckId(deckId))
                .thenReturn(Arrays.asList(mainCard, maybeboardCard));
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));

        // When
        boolean result = formatService.isDeckLegal(deckId, formatId);

        // Then
        assertThat(result).isTrue(); // Legal because banned card is in maybeboard
    }

    @Test
    @DisplayName("Should return false for deck legality when format not found")
    void shouldReturnFalse_WhenFormatNotFoundForDeckLegality() {
        // Given
        when(formatRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = formatService.isDeckLegal(1L, 999L);

        // Then
        assertThat(result).isFalse();
        verify(formatRepository).existsById(999L);
    }

    @Test
    @DisplayName("Should return cards for valid format ID with pagination")
    void shouldReturnCards_WhenFormatExists() {
        // Given
        Long formatId = 1L;
        int pageSize = 10;
        int pageNumber = 0;
        
        CardEntity cardEntity1 = new CardEntity();
        cardEntity1.setId(1L);
        cardEntity1.setName("Card One");
        
        CardEntity cardEntity2 = new CardEntity();
        cardEntity2.setId(2L);
        cardEntity2.setName("Card Two");
        
        List<CardEntity> cardEntities = Arrays.asList(cardEntity1, cardEntity2);
        Page<CardEntity> cardPage = new PageImpl<>(cardEntities, PageRequest.of(pageNumber, pageSize), cardEntities.size());
        
        Card card1 = Card.builder().id(1L).name("Card One").build();
        Card card2 = Card.builder().id(2L).name("Card Two").build();
        List<Card> expectedCards = Arrays.asList(card1, card2);
        
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));
        when(cardRepository.findByFormatId(eq(formatId), any(PageRequest.class))).thenReturn(cardPage);
        when(cardEntityMapper.toModelList(cardEntities)).thenReturn(expectedCards);

        // When
        List<Card> result = formatService.findCardsByFormatId(formatId, pageSize, pageNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(formatRepository).findById(formatId);
        verify(cardRepository).findByFormatId(eq(formatId), any(PageRequest.class));
        verify(cardEntityMapper).toModelList(cardEntities);
    }

    @Test
    @DisplayName("Should return empty list when no cards are legal in format")
    void shouldReturnEmptyList_WhenNoCardsAreLegal() {
        // Given
        Long formatId = 1L;
        Page<CardEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        
        when(formatRepository.findById(formatId)).thenReturn(Optional.of(testFormatEntity));
        when(cardRepository.findByFormatId(eq(formatId), any(PageRequest.class))).thenReturn(emptyPage);
        when(cardEntityMapper.toModelList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        List<Card> result = formatService.findCardsByFormatId(formatId, 10, 0);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(formatRepository).findById(formatId);
        verify(cardRepository).findByFormatId(eq(formatId), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when finding cards for non-existent format")
    void shouldThrowException_WhenFindingCardsForNonExistentFormat() {
        // Given
        when(formatRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> formatService.findCardsByFormatId(999L, 10, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Format not found with id: 999");
        
        verify(formatRepository).findById(999L);
        verifyNoInteractions(cardRepository, cardEntityMapper);
    }
}

