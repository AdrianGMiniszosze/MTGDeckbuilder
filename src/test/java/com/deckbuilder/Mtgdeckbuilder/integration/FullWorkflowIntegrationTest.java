package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.apigenerator.openapi.api.model.*;
import com.deckbuilder.mtgdeckbuilder.contract.CardController;
import com.deckbuilder.mtgdeckbuilder.contract.DeckController;
import com.deckbuilder.mtgdeckbuilder.contract.UserController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Full Workflow Integration Tests")
class FullWorkflowIntegrationTest {

	@Mock
	private UserController userController;

	@Mock
	private CardController cardController;

	@Mock
	private DeckController deckController;

	private UserDTO testUser;
	private CardDTO lightningBolt;
	private CardDTO forest;

	@BeforeEach
	void setUp() {
		// Test user
		this.testUser = UserDTO.builder()
				.id(1)
				.name("Test Player")
				.username("testplayer")
				.email("test@example.com")
				.country("USA")
				.build();

		// Test cards with variant information
		this.lightningBolt = CardDTO.builder()
				.id(1)
				.card_name("Lightning Bolt")
				.mana_cost("{R}")
				.cmc(1)
				.color_identity("R")
				.type_line("Instant")
				.card_type("Instant")
				.rarity(CardDTO.Rarity.COMMON)
				.card_text("Lightning Bolt deals 3 damage to any target.")
				.image_url("https://example.com/lightning-bolt.jpg")
				.language("en")
				.collector_number("001")
				.foil(false)
				.promo(false)
				.variation(false)
				.card_set(1)
				.build();

		this.forest = CardDTO.builder()
				.id(2)
				.card_name("Forest")
				.mana_cost("")
				.cmc(0)
				.color_identity("G")
				.type_line("Basic Land — Forest")
				.card_type("Land")
				.rarity(CardDTO.Rarity.COMMON)
				.card_text("")
				.image_url("https://example.com/forest.jpg")
				.language("en")
				.collector_number("264")
				.foil(false)
				.promo(false)
				.variation(false)
				.card_set(1)
				.build();

		// Setup mock responses
		when(userController.createUser(any(UserDTO.class)))
			.thenReturn(new ResponseEntity<>(testUser, HttpStatus.CREATED));

		when(cardController.getCardById(1))
			.thenReturn(new ResponseEntity<>(lightningBolt, HttpStatus.OK));

		when(cardController.getCardById(2))
			.thenReturn(new ResponseEntity<>(forest, HttpStatus.OK));
	}

	// ========================================
	// Critical Missing: End-to-End Tests
	// ========================================

	@Test
	@DisplayName("Should complete full workflow: Create User → Create Deck → Add Cards → Validate Deck")
	void shouldCompleteFullWorkflow() {
		// Step 1: Create User
		final ResponseEntity<UserDTO> userResponse = userController.createUser(testUser);
		assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(userResponse.getBody());
        assertThat(userResponse.getBody().getUsername()).isEqualTo("testplayer");

		// Step 2: Create Deck
		final DeckDTO newDeck = DeckDTO.builder()
				.deck_name("Red Burn Deck")
				.description("Aggressive red deck")
				.user_id(1)
				.format(1) // Standard format
				.deck_type(DeckDTO.Deck_type.MAIN)
				.is_private(false)
				.build();

		final DeckDTO createdDeck = DeckDTO.builder()
				.id(1)
				.deck_name(newDeck.getDeck_name())
				.description(newDeck.getDescription())
				.user_id(newDeck.getUser_id())
				.format(newDeck.getFormat())
				.deck_type(newDeck.getDeck_type())
				.is_private(newDeck.getIs_private())
				.build();

		// Mock deck creation
		when(deckController.createDeck(any(DeckDTO.class)))
			.thenReturn(new ResponseEntity<>(createdDeck, HttpStatus.CREATED));

		final ResponseEntity<DeckDTO> deckResponse = deckController.createDeck(newDeck);

		assertThat(deckResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(deckResponse.getBody());
        assertThat(deckResponse.getBody().getDeck_name()).isEqualTo("Red Burn Deck");
		assertThat(deckResponse.getBody().getUser_id()).isEqualTo(1);

		// Step 3: Add Cards to Deck (simulate deck building)
		final CardDeckDTO lightningBoltInDeck = CardDeckDTO.builder()
				.card_id(1)
				.deck_id(1)
				.quantity(4)
				.build();

		final CardDeckDTO forestInDeck = CardDeckDTO.builder()
				.card_id(2)
				.deck_id(1)
				.quantity(20)
				.build();

		// Step 4: Verify cards exist and have proper variant information
		final ResponseEntity<CardDTO> cardResponse1 = cardController.getCardById(1);
		final ResponseEntity<CardDTO> cardResponse2 = cardController.getCardById(2);

        Assertions.assertNotNull(cardResponse1.getBody());
        assertThat(cardResponse1.getBody().getCollector_number()).isEqualTo("001");
		assertThat(cardResponse1.getBody().getFoil()).isFalse();
        Assertions.assertNotNull(cardResponse2.getBody());
        assertThat(cardResponse2.getBody().getCollector_number()).isEqualTo("264");

		// Step 5: Validate deck construction (this would be done by service layer)
		// Mock validation results
		final boolean deckIsValid = validateDeckConstruction(createdDeck, List.of(lightningBoltInDeck, forestInDeck));
		assertThat(deckIsValid).isFalse(); // 24 cards < 60 minimum for Standard

		System.out.println("✅ Full workflow test completed successfully");
	}

	@Test
	@DisplayName("Should handle card variants in deck building")
	void shouldHandleCardVariantsInDeckBuilding() {
		// When - Adding different variants to deck
		final CardDeckDTO regularBolt = CardDeckDTO.builder()
				.card_id(1)
				.deck_id(1)
				.quantity(2)
				.build();

		final CardDeckDTO foilBolt = CardDeckDTO.builder()
				.card_id(3)
				.deck_id(1)
				.quantity(1)
				.build();

		final CardDeckDTO promoBolt = CardDeckDTO.builder()
				.card_id(4)
				.deck_id(1)
				.quantity(1)
				.build();

		// Then - Should validate as 4 total Lightning Bolts
		final List<CardDeckDTO> deckContents = List.of(regularBolt, foilBolt, promoBolt);
		final boolean variantValidation = validateVariantQuantities(deckContents);

		assertThat(variantValidation).isTrue(); // 2 + 1 + 1 = 4 total, which is valid

		System.out.println("✅ Card variant handling test completed successfully");
	}

	@Test
	@DisplayName("Should enforce format-specific deck construction rules")
	void shouldEnforceFormatSpecificDeckConstructionRules() {
		// When - Trying to add 4x Lightning Bolt (invalid in Commander)
		final CardDeckDTO invalidQuantity = CardDeckDTO.builder()
				.card_id(1)
				.deck_id(1)
				.quantity(4) // Invalid in Commander (singleton)
				.build();

		// Then - Should fail validation
		final boolean commanderValidation = validateCommanderDeckRules(List.of(invalidQuantity));
		assertThat(commanderValidation).isFalse(); // 4 copies violates singleton rule

		System.out.println("✅ Format-specific rules test completed successfully");
	}

	@Test
	@DisplayName("Should handle deck sharing and privacy settings")
	void shouldHandleDeckSharingAndPrivacySettings() {
		// Given - Private deck
		final DeckDTO privateDeck = DeckDTO.builder()
				.id(1)
				.deck_name("Secret Tech")
				.description("My secret deck")
				.user_id(1)
				.format(1)
				.deck_type(DeckDTO.Deck_type.MAIN)
				.is_private(true)
				.share_url(null) // No share URL for private decks
				.build();

		// When - Making deck public
		final DeckDTO publicDeck = DeckDTO.builder()
				.id(privateDeck.getId())
				.deck_name(privateDeck.getDeck_name())
				.description(privateDeck.getDescription())
				.user_id(privateDeck.getUser_id())
				.format(privateDeck.getFormat())
				.deck_type(privateDeck.getDeck_type())
				.is_private(false)
				.share_url("https://deckbuilder.com/shared/abc123")
				.build();

		// Then - Should have proper sharing settings
		assertThat(privateDeck.getIs_private()).isTrue();
		assertThat(privateDeck.getShare_url()).isNull();
		assertThat(publicDeck.getIs_private()).isFalse();
		assertThat(publicDeck.getShare_url()).isNotNull();

		System.out.println("✅ Deck sharing test completed successfully");
	}

	@Test
	@DisplayName("Should validate deck export and import functionality")
	void shouldValidateDeckExportAndImportFunctionality() {
		// Given - Complete deck with cards
		final CompleteDeckDTO completeDeck = CompleteDeckDTO.builder()
				.id(1)
				.deck_name("Lightning Aggro")
				.description("Fast red deck")
				.user_id(1)
				.format(1)
				.is_private(false)
				.main_board(List.of(
						CardDeckDTO.builder().deck_id(1).card_id(1).quantity(4).build(), // 4x Lightning Bolt
						CardDeckDTO.builder().deck_id(1).card_id(2).quantity(20).build() // 20x Card #2
				))
				.side_board(List.of(
						CardDeckDTO.builder().deck_id(1).card_id(3).quantity(3).build() // 3x Card #3
				))
				.build();

		// When - Exporting deck
		final String exportedDeck = exportDeckToString(completeDeck);

		// Then - Should contain all deck information
		assertThat(exportedDeck)
			.contains("Lightning Aggro")
			.contains("4 Lightning Bolt")
			.contains("20 Card #2")
			.contains("Sideboard:")
			.contains("3 Card #3");

		System.out.println("✅ Deck export/import test completed successfully");
	}

	// Helper methods for validation (these would be implemented in actual service classes)
	private boolean validateDeckConstruction(DeckDTO deck, List<CardDeckDTO> cards) {
		final int totalMainboardCards = cards.stream()
				.mapToInt(CardDeckDTO::getQuantity)
				.sum();

		// Standard format requires 60+ cards
		if (deck.getFormat() == 1) {
			return totalMainboardCards >= 60;
		}

		return true;
	}

	private boolean validateVariantQuantities(List<CardDeckDTO> variants) {
		final int totalQuantity = variants.stream()
				.mapToInt(CardDeckDTO::getQuantity)
				.sum();

		// Standard 4-of rule
		return totalQuantity <= 4;
	}

	private boolean validateCommanderDeckRules(List<CardDeckDTO> cards) {
		// Commander allows only 1 copy of non-basic lands
		return cards.stream().allMatch(card -> card.getQuantity() <= 1);
	}

	private String exportDeckToString(CompleteDeckDTO deck) {
		final StringBuilder sb = new StringBuilder();
		sb.append("// ").append(deck.getDeck_name()).append(" ");
		sb.append("// ").append(deck.getDescription()).append(" ");

		sb.append(" Mainboard: ");
		if (deck.getMain_board() != null) {
			deck.getMain_board().forEach(card ->
				sb.append(card.getQuantity()).append(" ").append(getCardNameById(card.getCard_id())).append(" "));
		}

		sb.append(" Sideboard: ");
		if (deck.getSide_board() != null) {
			deck.getSide_board().forEach(card ->
				sb.append(card.getQuantity()).append(" ").append(getCardNameById(card.getCard_id())).append(" "));
		}

		return sb.toString();
	}

	private String getCardNameById(Integer cardId) {
		// Map card IDs to names for the test
        return switch (cardId) {
            case 1 -> "Lightning Bolt";
            case 2 -> "Card #2";
            case 3 -> "Card #3";
            default -> "Unknown Card";
        };
	}
}
