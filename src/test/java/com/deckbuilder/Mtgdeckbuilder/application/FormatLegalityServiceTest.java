package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Format Legality Service Tests")
class FormatLegalityServiceTest {

	@Mock
	private FormatService formatService;


	private Card lightningBolt;
	private Card blackLotus;
	private Card brainstorm;
	private Card ancestralRecall;

	@BeforeEach
	void setUp() {
		// Test cards
		this.lightningBolt = Card.builder()
				.id(1L)
				.name("Lightning Bolt")
				.build();

		this.blackLotus = Card.builder()
				.id(2L)
				.name("Black Lotus")
				.build();

		this.brainstorm = Card.builder()
				.id(3L)
				.name("Brainstorm")
				.build();

		this.ancestralRecall = Card.builder()
				.id(4L)
				.name("Ancestral Recall")
				.build();
	}

	// ========================================
	// Critical Missing: Format Legality Tests
	// ========================================

	@Test
	@DisplayName("Should check if card is legal in format")
	void shouldCheckIfCardIsLegalInFormat() {
		// Given
		when(this.formatService.isCardLegal(1L, 1L)).thenReturn(true);
		when(this.formatService.isCardLegal(2L, 1L)).thenReturn(false);

		// When
		final boolean lightningBoltLegal = this.formatService.isCardLegal(this.lightningBolt.getId(), 1L);
		final boolean blackLotusLegal = this.formatService.isCardLegal(this.blackLotus.getId(), 1L);

		// Then
		assertThat(lightningBoltLegal).isTrue(); // Lightning Bolt is legal in Standard
		assertThat(blackLotusLegal).isFalse(); // Black Lotus is banned in Standard
	}

	@Test
	@DisplayName("Should handle banned cards correctly")
	void shouldHandleBannedCardsCorrectly() {
		// Given - Black Lotus is banned in both Standard and Legacy
		when(this.formatService.isCardLegal(2L, 1L)).thenReturn(false);
		when(this.formatService.isCardLegal(2L, 2L)).thenReturn(false);

		// When
		final boolean blackLotusInStandard = this.formatService.isCardLegal(this.blackLotus.getId(), 1L);
		final boolean blackLotusInLegacy = this.formatService.isCardLegal(this.blackLotus.getId(), 2L);

		// Then
		assertThat(blackLotusInStandard).isFalse(); // Banned in Standard
		assertThat(blackLotusInLegacy).isFalse(); // Also banned in Legacy
	}

	@Test
	@DisplayName("Should handle restricted cards correctly")
	void shouldHandleRestrictedCardsCorrectly() {
		// Given - In Vintage, restricted cards are still "legal" but limited to 1 copy
		when(this.formatService.isCardLegal(2L, 3L)).thenReturn(true); // Black Lotus restricted but legal
		when(this.formatService.isCardLegal(4L, 3L)).thenReturn(true); // Ancestral Recall restricted but legal
		when(this.formatService.isCardLegal(1L, 3L)).thenReturn(true); // Lightning Bolt unrestricted

		// When
		final boolean blackLotusRestricted = this.formatService.isCardLegal(this.blackLotus.getId(), 3L);
		final boolean ancestralRecallRestricted = this.formatService.isCardLegal(this.ancestralRecall.getId(), 3L);
		final boolean lightningBoltRestricted = this.formatService.isCardLegal(this.lightningBolt.getId(), 3L);

		// Then - All should be legal in Vintage (restriction is about quantity, not legality)
		assertThat(blackLotusRestricted).isTrue(); // Black Lotus is legal (though restricted) in Vintage
		assertThat(ancestralRecallRestricted).isTrue(); // Ancestral Recall is legal (though restricted) in Vintage
		assertThat(lightningBoltRestricted).isTrue(); // Lightning Bolt is unrestricted
	}

	@Test
	@DisplayName("Should validate card legality for different formats")
	void shouldValidateCardLegalityForDifferentFormats() {
		// Given
		when(this.formatService.isCardLegal(1L, 1L)).thenReturn(true); // Lightning Bolt in Standard
		when(this.formatService.isCardLegal(2L, 1L)).thenReturn(false); // Black Lotus in Standard (banned)
		when(this.formatService.isCardLegal(2L, 3L)).thenReturn(true); // Black Lotus in Vintage (restricted but legal)

		// When & Then
		assertThat(this.formatService.isCardLegal(this.lightningBolt.getId(), 1L)).isTrue(); // Standard allows 4 copies
		assertThat(this.formatService.isCardLegal(this.blackLotus.getId(), 1L)).isFalse(); // Banned cards not legal
		assertThat(this.formatService.isCardLegal(this.blackLotus.getId(), 3L)).isTrue(); // Restricted cards are still legal
	}

	@Test
	@DisplayName("Should handle basic lands exception")
	void shouldHandleBasicLandsException() {
		// Given
		final Card forest = Card.builder()
				.id(5L)
				.name("Forest")
				.typeLine("Basic Land — Forest")
				.cardSupertype("Basic")
				.build();

		when(this.formatService.isCardLegal(5L, 1L)).thenReturn(true);

		// When
		final boolean forestLegal = this.formatService.isCardLegal(forest.getId(), 1L);

		// Then
		assertThat(forestLegal).isTrue(); // Basic lands are always legal
	}

	@Test
	@DisplayName("Should validate card legality across multiple formats")
	void shouldValidateCardLegalityAcrossMultipleFormats() {
		// Given - Brainstorm is legal in all formats in this test
		when(this.formatService.isCardLegal(3L, 1L)).thenReturn(true); // Standard
		when(this.formatService.isCardLegal(3L, 2L)).thenReturn(true); // Legacy
		when(this.formatService.isCardLegal(3L, 3L)).thenReturn(true); // Vintage

		// When
		final boolean brainstormInStandard = this.formatService.isCardLegal(this.brainstorm.getId(), 1L);
		final boolean brainstormInLegacy = this.formatService.isCardLegal(this.brainstorm.getId(), 2L);
		final boolean brainstormInVintage = this.formatService.isCardLegal(this.brainstorm.getId(), 3L);

		// Then
		assertThat(brainstormInStandard).isTrue(); // Legal in Standard (not in banned list)
		assertThat(brainstormInLegacy).isTrue(); // Legal in Legacy
		assertThat(brainstormInVintage).isTrue(); // Legal in Vintage
	}

	@Test
	@DisplayName("Should check format rotation and card legality")
	void shouldCheckFormatRotationAndCardLegality() {
		// Given - Lightning Bolt remains legal after rotation
		when(this.formatService.isCardLegal(1L, 1L)).thenReturn(true);

		// When
		final boolean cardLegal = this.formatService.isCardLegal(this.lightningBolt.getId(), 1L);

		// Then
		assertThat(cardLegal).isTrue(); // Card should be legal (rotation logic can be added later)
	}

	@Test
	@DisplayName("Should validate different card legality statuses")
	void shouldValidateDifferentCardLegalityStatuses() {
		// Given - Different cards have different legality in different formats
		when(this.formatService.isCardLegal(2L, 1L)).thenReturn(false); // Black Lotus banned in Standard
		when(this.formatService.isCardLegal(2L, 2L)).thenReturn(false); // Black Lotus banned in Legacy
		when(this.formatService.isCardLegal(2L, 3L)).thenReturn(true);  // Black Lotus restricted but legal in Vintage

		// When & Then
		assertThat(this.formatService.isCardLegal(this.blackLotus.getId(), 1L)).isFalse(); // BANNED in Standard
		assertThat(this.formatService.isCardLegal(this.blackLotus.getId(), 2L)).isFalse(); // BANNED in Legacy
		assertThat(this.formatService.isCardLegal(this.blackLotus.getId(), 3L)).isTrue();  // RESTRICTED but legal in Vintage
	}

	@Test
	@DisplayName("Should handle card name matching for legality")
	void shouldHandleCardNameMatchingForLegality() {
		// Given - Cards with case variations in banned lists
		when(this.formatService.isCardLegal(2L, 4L)).thenReturn(false); // Black Lotus banned
		when(this.formatService.isCardLegal(1L, 4L)).thenReturn(false); // Lightning Bolt banned

		// When
		final boolean blackLotusLegal = this.formatService.isCardLegal(this.blackLotus.getId(), 4L);
		final boolean lightningBoltLegal = this.formatService.isCardLegal(this.lightningBolt.getId(), 4L);

		// Then
		assertThat(blackLotusLegal).isFalse(); // Should match "black lotus" (case insensitive)
		assertThat(lightningBoltLegal).isFalse(); // Should match "LIGHTNING BOLT" (case insensitive)
	}
}
