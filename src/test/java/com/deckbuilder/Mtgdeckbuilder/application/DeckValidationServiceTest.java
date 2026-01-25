package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.DeckValidationServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardLegalityRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Validation Service Tests")
class DeckValidationServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private FormatRepository formatRepository;

    @Mock
    private CardLegalityRepository cardLegalityRepository;

    @Mock
    private CardInDeckRepository cardInDeckRepository;

    @InjectMocks
    private DeckValidationServiceImpl deckValidationService;

    private DeckEntity testDeck;
    private CardEntity regularCard;
    private CardEntity basicLand;
    private CardEntity bannedCard;
    private FormatEntity standardFormat;
    private CardLegalityEntity bannedLegality;

    @BeforeEach
    void setUp() {
        // Test deck
        testDeck = new DeckEntity();
        testDeck.setId(1L);
        testDeck.setFormatId(1L);

        // Regular card (Lightning Bolt)
        regularCard = new CardEntity();
        regularCard.setId(1L);
        regularCard.setName("Lightning Bolt");
        regularCard.setCardType("Instant");
        regularCard.setCardSupertype(null);
        regularCard.setUnlimitedCopies(false);

        // Basic land (Mountain)
        basicLand = new CardEntity();
        basicLand.setId(2L);
        basicLand.setName("Mountain");
        basicLand.setCardType("Land");
        basicLand.setCardSupertype("Basic");
        basicLand.setUnlimitedCopies(false);

        // Banned card
        bannedCard = new CardEntity();
        bannedCard.setId(3L);
        bannedCard.setName("Black Lotus");
        bannedCard.setCardType("Artifact");
        bannedCard.setCardSupertype(null);
        bannedCard.setUnlimitedCopies(false);

        // Restricted card
        CardEntity restrictedCard = new CardEntity();
        restrictedCard.setId(4L);
        restrictedCard.setName("Ancestral Recall");
        restrictedCard.setCardType("Instant");
        restrictedCard.setCardSupertype(null);
        restrictedCard.setUnlimitedCopies(false);

        // Formats
        standardFormat = FormatEntity.builder()
            .id(1L)
            .name("Standard")
            .maxDeckSize(60)
            .maxSideboardSize(15)
            .build();

        FormatEntity commanderFormat = FormatEntity.builder()
                .id(2L)
                .name("Commander")
                .maxDeckSize(100)
                .maxSideboardSize(0)
                .build();

        // Card legalities
        bannedLegality = new CardLegalityEntity();
        bannedLegality.setCardId(3L);
        bannedLegality.setFormatId(1L);
        bannedLegality.setLegalityStatus("banned");

        CardLegalityEntity restrictedLegality = new CardLegalityEntity();
        restrictedLegality.setCardId(4L);
        restrictedLegality.setFormatId(1L);
        restrictedLegality.setLegalityStatus("restricted");
    }

    @Test
    @DisplayName("Should inject service correctly")
    void shouldInjectServiceCorrectly() {
        assertThat(deckValidationService).isNotNull();
    }

    @Test
    @DisplayName("Should allow valid card addition")
    void shouldAllowValidCardAddition() {
        // Given
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeck));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(regularCard));
        when(cardLegalityRepository.findByCardIdAndFormatId(1L, 1L)).thenReturn(Optional.empty());
        when(formatRepository.findById(1L)).thenReturn(Optional.of(standardFormat));
        when(cardInDeckRepository.sumQuantityByDeckIdAndSection(1L, "main")).thenReturn(0);

        // When & Then - Should not throw exception
        deckValidationService.validateCardAddition(1L, 1L, 4, "main", false);

        // Verify interactions - After optimization, in validateCardAddition only:
        // 1. cardRepository.findById called once in validateCardAddition
        // 2. cardLegalityRepository.findByCardIdAndFormatId called once (optimized!)
        // 3. formatRepository.findById called once in private getMaxAllowedQuantity
        verify(deckRepository).findById(1L);
        verify(cardRepository, times(1)).findById(1L);
        verify(cardLegalityRepository, times(1)).findByCardIdAndFormatId(1L, 1L);
    }

    @Test
    @DisplayName("Should skip validation for maybeboard")
    void shouldSkipValidationForMaybeboard() {
        // When & Then - Should not throw exception or call any repositories
        deckValidationService.validateCardAddition(1L, 1L, 100, "maybeboard", false);

        // Verify no validation calls were made
        verifyNoInteractions(deckRepository, cardRepository, cardLegalityRepository, formatRepository);
    }

    @Test
    @DisplayName("Should throw exception for banned card")
    void shouldThrowExceptionForBannedCard() {
        // Given
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeck));
        when(cardRepository.findById(3L)).thenReturn(Optional.of(bannedCard));
        when(cardLegalityRepository.findByCardIdAndFormatId(3L, 1L)).thenReturn(Optional.of(bannedLegality));
        when(formatRepository.findById(1L)).thenReturn(Optional.of(standardFormat));

        // When & Then
        assertThatThrownBy(() -> deckValidationService.validateCardAddition(1L, 3L, 1, "main", false))
            .isInstanceOf(InvalidDeckCompositionException.class)
            .hasMessageContaining("banned");

        // Verify interactions - for banned cards, it fails early so cardRepository.findById is only called once
        verify(deckRepository).findById(1L);
        verify(cardRepository).findById(3L);
        verify(cardLegalityRepository).findByCardIdAndFormatId(3L, 1L);
    }

    @Test
    @DisplayName("Should allow unlimited basic lands")
    void shouldAllowUnlimitedBasicLands() {
        // Given
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeck));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(basicLand));
        when(cardLegalityRepository.findByCardIdAndFormatId(2L, 1L)).thenReturn(Optional.empty());
        when(formatRepository.findById(1L)).thenReturn(Optional.of(standardFormat));
        when(cardInDeckRepository.sumQuantityByDeckIdAndSection(1L, "main")).thenReturn(40); // Already 40 cards in deck

        // When & Then - Should allow any quantity for basic lands (20 would exceed normal 4-card limit)
        // This should NOT throw an exception even though we're adding 20 copies of a card
        deckValidationService.validateCardAddition(1L, 2L, 20, "main", false);

        // Also verify that getMaxAllowedQuantity returns unlimited for basic lands
        int maxAllowed = deckValidationService.getMaxAllowedQuantity(2L, 1L);
        assertThat(maxAllowed).isEqualTo(9999); // UNLIMITED_QUANTITY constant used by service

        // Verify interactions - This test makes TWO separate API calls:
        // 1. validateCardAddition() - optimized internally
        // 2. getMaxAllowedQuantity() - separate public API call
        // So cardRepository.findById is called twice total (once per API call)
        verify(deckRepository).findById(1L);
        verify(cardRepository, times(2)).findById(2L);
        verify(cardLegalityRepository, atLeast(1)).findByCardIdAndFormatId(2L, 1L);
    }

    @Test
    @DisplayName("Should return main deck size for getMaxDeckSize")
    void shouldReturnMainDeckSizeForGetMaxDeckSize() {
        // Given
        when(formatRepository.findById(1L)).thenReturn(Optional.of(standardFormat));

        // When
        int maxDeckSize = deckValidationService.getMaxDeckSize(1L, "main");

        // Then
        assertThat(maxDeckSize).isEqualTo(60);
    }

    @Test
    @DisplayName("Should return 4 for regular cards max quantity")
    void shouldReturn4ForRegularCardsMaxQuantity() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(regularCard));
        when(cardLegalityRepository.findByCardIdAndFormatId(1L, 1L)).thenReturn(Optional.empty());
        when(formatRepository.findById(1L)).thenReturn(Optional.of(standardFormat));

        // When
        int maxQuantity = deckValidationService.getMaxAllowedQuantity(1L, 1L);

        // Then
        assertThat(maxQuantity).isEqualTo(4);
    }

    @Test
    @DisplayName("Should throw exception for non-basic card quantity > 4")
    void shouldThrowExceptionForNonBasicCardQuantityGreaterThan4() {
        // Given
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeck));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(regularCard));
        when(cardLegalityRepository.findByCardIdAndFormatId(1L, 1L)).thenReturn(Optional.empty());
        when(formatRepository.findById(1L)).thenReturn(Optional.of(standardFormat));

        // When & Then - Should throw exception for trying to add 5 copies of a non-basic card
        assertThatThrownBy(() -> deckValidationService.validateCardAddition(1L, 1L, 5, "main", false))
            .isInstanceOf(InvalidDeckCompositionException.class)
            .hasMessageContaining("quantity limit");
    }
}
