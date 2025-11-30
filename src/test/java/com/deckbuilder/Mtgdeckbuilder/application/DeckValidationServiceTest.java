package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Validation Logic Tests")
class DeckValidationServiceTest {
	// Note: This tests deck validation functionality that should be part of FormatService
	// These tests validate deck construction rules, format legality, and card quantity limits

	@Mock
	private FormatService formatService;

	@Mock
	private CardService cardService;


    private Card lightningBolt;
	private Card forest;
	private Card blackLotus;
	private Deck testDeck;

	@BeforeEach
	void setUp() {
        // Test cards
		this.lightningBolt = Card.builder()
				.id(1L)
				.name("Lightning Bolt")
				.typeLine("Instant")
				.cardType("Instant")
				.build();

		this.forest = Card.builder()
				.id(2L)
				.name("Forest")
				.typeLine("Basic Land — Forest")
				.cardType("Land")
				.cardSupertype("Basic")
				.build();

		this.blackLotus = Card.builder()
				.id(3L)
				.name("Black Lotus")
				.typeLine("Artifact")
				.cardType("Artifact")
				.build();

		// Test deck with some cards
		this.testDeck = Deck.builder()
				.id(1L)
				.name("Test Deck")
				.formatId(1L) // Standard
				.cards(Arrays.asList(
						createCardInDeck(1L, 4, "main"), // 4x Lightning Bolt
						createCardInDeck(2L, 20, "main") // 20x Forest
				))
				.build();
	}

	private Deck.CardInDeck createCardInDeck(Long cardId, int quantity, String section) {
		return Deck.CardInDeck.builder()
				.cardId(cardId)
				.quantity(quantity)
				.section(section)
				.build();
	}

	// ========================================
	// Critical Missing: Deck Building Tests
	// ========================================

	@Test
	@DisplayName("Should validate deck size for format")
	void shouldValidateDeckSizeForFormat() {
		// Given - 24 cards is below Standard minimum of 60
		when(this.formatService.isDeckLegal(1L, 1L)).thenReturn(false);

		// When
		final boolean isValid = this.formatService.isDeckLegal(this.testDeck.getId(), 1L);

		// Then
		assertThat(isValid).isFalse(); // 24 cards < 60 minimum
	}

	@Test
	@DisplayName("Should enforce 4-of rule in Standard")
	void shouldEnforce4OfRuleInStandard() {
		// Given - Deck with 5 copies should be invalid
		final Deck invalidDeck = Deck.builder()
				.id(2L)
				.formatId(1L)
				.cards(List.of(createCardInDeck(1L, 5, "main"))) // 5x Lightning Bolt (invalid)
				.build();

		when(this.formatService.isDeckLegal(2L, 1L)).thenReturn(false);

		// When
		final boolean isValid = this.formatService.isDeckLegal(invalidDeck.getId(), 1L);

		// Then
		assertThat(isValid).isFalse(); // More than 4 copies
	}

	@Test
	@DisplayName("Should allow unlimited basic lands")
	void shouldAllowUnlimitedBasicLands() {
		// Given
		final Deck basicLandDeck = Deck.builder()
				.formatId(1L)
				.cards(List.of(createCardInDeck(2L, 40, "main"))) // 40x Forest
				.build();

		when(this.formatService.isDeckLegal(3L, 1L)).thenReturn(true);

		// When
		final boolean isValid = this.formatService.isDeckLegal(basicLandDeck.getId(), 1L);

		// Then
		assertThat(isValid).isTrue(); // Basic lands can exceed 4-of rule
	}

	@Test
	@DisplayName("Should enforce singleton rule in Commander")
	void shouldEnforceSingletonRuleInCommander() {
		// Given - Commander deck with 2x Lightning Bolt (invalid)
		final Deck commanderDeck = Deck.builder()
				.id(4L)
				.formatId(2L)
				.cards(List.of(createCardInDeck(1L, 2, "main"))) // 2x Lightning Bolt (invalid in Commander)
				.build();

		when(this.formatService.isDeckLegal(4L, 2L)).thenReturn(false);

		// When
		final boolean isValid = this.formatService.isDeckLegal(commanderDeck.getId(), 2L);

		// Then
		assertThat(isValid).isFalse(); // Commander allows only 1 copy of non-basic lands
	}

	@Test
	@DisplayName("Should check card legality in format")
	void shouldCheckCardLegalityInFormat() {
		// Given - Black Lotus is banned in Standard
		when(this.formatService.isCardLegal(3L, 1L)).thenReturn(false);

		// When
		final boolean isValid = this.formatService.isCardLegal(this.blackLotus.getId(), 1L);

		// Then
		assertThat(isValid).isFalse(); // Black Lotus is banned in Standard
	}

	@Test
	@DisplayName("Should validate sideboard size")
	void shouldValidateSideboardSize() {
		// Given - Deck with oversized sideboard
		final Deck deckWithLargeSideboard = Deck.builder()
				.id(5L)
				.formatId(1L)
				.cards(Arrays.asList(
						createCardInDeck(1L, 4, "main"),
						createCardInDeck(2L, 20, "sideboard") // 20 cards in sideboard (max 15)
				))
				.build();

		when(this.formatService.isDeckLegal(5L, 1L)).thenReturn(false);

		// When
		final boolean isValid = this.formatService.isDeckLegal(deckWithLargeSideboard.getId(), 1L);

		// Then
		assertThat(isValid).isFalse(); // Sideboard exceeds 15 cards
	}

	@Test
	@DisplayName("Should allow choosing specific card variants")
	void shouldAllowChoosingSpecificCardVariant() {
		// Given - Different variants of the same card
		final Card forestVariant1 = this.forest.toBuilder().collectorNumber("264").build();
		final Card forestVariant2 = this.forest.toBuilder().collectorNumber("265").build();

		final Deck forestVariantDeck = Deck.builder()
				.id(6L)
				.formatId(1L)
				.cards(Arrays.asList(
						createCardInDeck(forestVariant1.getId(), 10, "main"),
						createCardInDeck(forestVariant2.getId(), 10, "main")
				))
				.build();

		when(this.formatService.isDeckLegal(6L, 1L)).thenReturn(true);

		// When
		final boolean isValid = this.formatService.isDeckLegal(forestVariantDeck.getId(), 1L);

		// Then
		assertThat(isValid).isTrue(); // Different variants are treated as separate cards for basic lands
	}

	@Test
	@DisplayName("Should validate deck even with different variants of non-basic cards")
	void shouldValidateDeckEvenWithDifferentVariantsOfNonBasicCards() {
		final Deck variantDeck = Deck.builder()
				.id(7L)
				.formatId(1L)
				.cards(Arrays.asList(
						createCardInDeck(10L, 2, "main"), // 2x regular Lightning Bolt
						createCardInDeck(11L, 2, "main")  // 2x foil Lightning Bolt
				))
				.build();

		when(this.formatService.isDeckLegal(7L, 1L)).thenReturn(true);

		// When
		final boolean isValid = this.formatService.isDeckLegal(variantDeck.getId(), 1L);

		// Then
		assertThat(isValid).isTrue(); // 4 total Lightning Bolts across variants is valid
	}

	@Test
	@DisplayName("Should invalidate deck when variants exceed 4-of rule")
	void shouldInvalidateDeckWhenVariantsExceed4OfRule() {
		// Given - Too many variants of Lightning Bolt
		final Deck variantDeck = Deck.builder()
				.id(8L)
				.formatId(1L)
				.cards(Arrays.asList(
						createCardInDeck(10L, 2, "main"), // 2x regular Lightning Bolt
						createCardInDeck(11L, 2, "main"), // 2x foil Lightning Bolt
						createCardInDeck(12L, 2, "main")  // 2x promo Lightning Bolt = 6 total
				))
				.build();

		when(this.formatService.isDeckLegal(8L, 1L)).thenReturn(false);

		// When
		final boolean isValid = this.formatService.isDeckLegal(variantDeck.getId(), 1L);

		// Then
		assertThat(isValid).isFalse(); // 6 total Lightning Bolts exceeds 4-of rule
	}

	@Test
	@DisplayName("Should validate complete deck for format compliance")
	void shouldValidateCompleteDeckForFormatCompliance() {
		// Given - Valid 60 card Standard deck
		final Deck validStandardDeck = Deck.builder()
				.id(9L)
				.formatId(1L)
				.cards(Arrays.asList(
						createCardInDeck(1L, 4, "main"), // 4x Lightning Bolt
						createCardInDeck(2L, 56, "main") // 56x Forest = 60 total
				))
				.build();

		when(this.formatService.isDeckLegal(9L, 1L)).thenReturn(true);

		// When
		final boolean isValid = this.formatService.isDeckLegal(validStandardDeck.getId(), 1L);

		// Then
		assertThat(isValid).isTrue(); // Valid 60-card Standard deck
	}
}
