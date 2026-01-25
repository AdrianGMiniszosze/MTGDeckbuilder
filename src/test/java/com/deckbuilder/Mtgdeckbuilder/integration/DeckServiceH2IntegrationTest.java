package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using H2 in-memory database for basic functionality testing.
 * This test doesn't require Docker and can run in environments where Testcontainers is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Deck Service Integration Tests with H2")
class DeckServiceH2IntegrationTest {

    @Autowired
    private DeckService deckService;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardInDeckRepository cardInDeckRepository;

    private UserEntity testUser;
    private FormatEntity standardFormat;
    private CardEntity testCard;

    @BeforeEach
    void setUp() {
        // Create test user with all required fields
        testUser = new UserEntity();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setHashedPassword("hashedpassword123");
        testUser.setCountry("US");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create test format
        standardFormat = FormatEntity.builder()
                .name("Standard")
                .description("Standard format")
                .minDeckSize(60)
                .maxDeckSize(60)
                .maxSideboardSize(15)
                .build();
        standardFormat = formatRepository.save(standardFormat);

        // Create test card
        testCard = new CardEntity();
        testCard.setName("Lightning Bolt");
        testCard.setManaCost("{R}");
        testCard.setCmc(1);
        testCard.setColorIdentity("R");
        testCard.setTypeLine("Instant");
        testCard.setCardType("Instant");
        testCard.setRarity("common");
        testCard.setCardText("Deal 3 damage to any target.");
        testCard.setImageUrl("http://example.com/lightning-bolt.jpg");
        testCard.setLanguage("en");
        testCard.setCollectorNumber("001");
        testCard.setUnlimitedCopies(false);
        testCard.setFoil(false);
        testCard.setGameChanger(false);
        testCard.setPromo(false);
        testCard.setVariation(false);
        testCard = cardRepository.save(testCard);
    }

    @Test
    @DisplayName("Should create deck successfully with H2")
    void shouldCreateDeckSuccessfully() {
        // Given
        String deckName = "Test Lightning Deck";
        String description = "A test deck with Lightning Bolt";

        Deck deck = Deck.builder()
                .name(deckName)
                .description(description)
                .userId(testUser.getId())
                .formatId(standardFormat.getId())
                .isPrivate(false)
                .build();

        // When
        Deck createdDeck = deckService.create(deck);

        // Then
        assertThat(createdDeck).isNotNull();
        assertThat(createdDeck.getId()).isNotNull();
        assertThat(createdDeck.getName()).isEqualTo(deckName);
        assertThat(createdDeck.getDescription()).isEqualTo(description);
        assertThat(createdDeck.getUserId()).isEqualTo(testUser.getId());
        assertThat(createdDeck.getFormatId()).isEqualTo(standardFormat.getId());
        assertThat(createdDeck.getCreated()).isNotNull();
        assertThat(createdDeck.getModified()).isNotNull();

        // Verify persistence in database
        DeckEntity savedDeck = deckRepository.findById(createdDeck.getId()).orElseThrow();
        assertThat(savedDeck.getName()).isEqualTo(deckName);
        assertThat(savedDeck.getDescription()).isEqualTo(description);
        assertThat(savedDeck.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedDeck.getFormatId()).isEqualTo(standardFormat.getId());
    }

    @Test
    @DisplayName("Should add cards to deck successfully")
    void shouldAddCardsToDeckSuccessfully() {
        // Given - Create a deck first
        Deck deck = Deck.builder()
                .name("Lightning Deck")
                .description("Deck with lightning bolt")
                .userId(testUser.getId())
                .formatId(standardFormat.getId())
                .isPrivate(false)
                .build();
        deck = deckService.create(deck);

        // When - Add cards to the deck
        Deck updatedDeck = deckService.addCard(deck.getId(), testCard.getId(), 4, "main");

        // Then
        assertThat(updatedDeck).isNotNull();
        assertThat(updatedDeck.getId()).isEqualTo(deck.getId());

        // Verify the card was added to the deck via repository
        List<CardInDeckEntity> cardsInDeck = cardInDeckRepository.findByDeckId(deck.getId());
        assertThat(cardsInDeck).hasSize(1);

        CardInDeckEntity addedCard = cardsInDeck.get(0);
        assertThat(addedCard.getCardId()).isEqualTo(testCard.getId());
        assertThat(addedCard.getDeckId()).isEqualTo(deck.getId());
        assertThat(addedCard.getQuantity()).isEqualTo(4);
        assertThat(addedCard.getSection()).isEqualTo("main");
    }

    @Test
    @DisplayName("Should remove cards from deck successfully")
    void shouldRemoveCardsFromDeckSuccessfully() {
        // Given - Create a deck and add cards first
        Deck deck = Deck.builder()
                .name("Lightning Deck")
                .description("Deck with lightning bolt")
                .userId(testUser.getId())
                .formatId(standardFormat.getId())
                .isPrivate(false)
                .build();
        deck = deckService.create(deck);

        // Add 4 cards
        deckService.addCard(deck.getId(), testCard.getId(), 4, "main");

        // When - Remove 2 cards from the deck
        Deck updatedDeck = deckService.removeCard(deck.getId(), testCard.getId(), 2, "main");

        // Then
        assertThat(updatedDeck).isNotNull();
        assertThat(updatedDeck.getId()).isEqualTo(deck.getId());

        // Verify the card quantity was updated
        List<CardInDeckEntity> cardsInDeck = cardInDeckRepository.findByDeckId(deck.getId());
        assertThat(cardsInDeck).hasSize(1);

        CardInDeckEntity updatedCard = cardsInDeck.get(0);
        assertThat(updatedCard.getQuantity()).isEqualTo(2); // 4 - 2 = 2
    }

    @Test
    @DisplayName("Should remove all cards when quantity reaches zero")
    void shouldRemoveAllCardsWhenQuantityReachesZero() {
        // Given - Create a deck and add cards first
        Deck deck = Deck.builder()
                .name("Lightning Deck")
                .description("Deck with lightning bolt")
                .userId(testUser.getId())
                .formatId(standardFormat.getId())
                .isPrivate(false)
                .build();
        deck = deckService.create(deck);

        // Add 3 cards
        deckService.addCard(deck.getId(), testCard.getId(), 3, "main");

        // When - Remove all 3 cards from the deck
        Deck updatedDeck = deckService.removeCard(deck.getId(), testCard.getId(), 3, "main");

        // Then
        assertThat(updatedDeck).isNotNull();

        // Verify the card was completely removed
        List<CardInDeckEntity> cardsInDeck = cardInDeckRepository.findByDeckId(deck.getId());
        assertThat(cardsInDeck).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple cards in different sections")
    void shouldHandleMultipleCardsInDifferentSections() {
        // Given - Create a deck first
        Deck deck = Deck.builder()
                .name("Mixed Deck")
                .description("Deck with cards in main and sideboard")
                .userId(testUser.getId())
                .formatId(standardFormat.getId())
                .isPrivate(false)
                .build();
        deck = deckService.create(deck);

        // When - Add cards to different sections
        deckService.addCard(deck.getId(), testCard.getId(), 4, "main");
        deckService.addCard(deck.getId(), testCard.getId(), 2, "sideboard");

        // Then
        List<CardInDeckEntity> cardsInDeck = cardInDeckRepository.findByDeckId(deck.getId());
        assertThat(cardsInDeck).hasSize(2);

        // Verify main deck card
        CardInDeckEntity mainCard = cardsInDeck.stream()
                .filter(c -> c.getSection().equals("main"))
                .findFirst()
                .orElseThrow();
        assertThat(mainCard.getQuantity()).isEqualTo(4);

        // Verify sideboard card
        CardInDeckEntity sideboardCard = cardsInDeck.stream()
                .filter(c -> c.getSection().equals("sideboard"))
                .findFirst()
                .orElseThrow();
        assertThat(sideboardCard.getQuantity()).isEqualTo(2);
    }
}
