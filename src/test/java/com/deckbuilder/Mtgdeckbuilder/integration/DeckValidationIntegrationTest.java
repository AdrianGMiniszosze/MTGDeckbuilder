package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardLegalityRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Deck Validation Repository Integration Tests")
@Transactional
class DeckValidationIntegrationTest {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardLegalityRepository cardLegalityRepository;

    @Autowired
    private CardInDeckRepository cardInDeckRepository;

    private DeckEntity testDeck;
    private CardEntity lightningBolt;
    private CardEntity basicMountain;
    private CardEntity bannedCard;
    private CardEntity restrictedCard;
    private FormatEntity standardFormat;
    private FormatEntity commanderFormat;
    private UserEntity testUser;

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

        // Create test formats
        standardFormat = FormatEntity.builder()
            .name("Standard")
            .description("Standard format")
            .minDeckSize(60)
            .maxDeckSize(60)
            .maxSideboardSize(15)
            .build();
        standardFormat = formatRepository.save(standardFormat);

        commanderFormat = FormatEntity.builder()
            .name("Commander")
            .description("Commander format")
            .minDeckSize(100)
            .maxDeckSize(100)
            .maxSideboardSize(0)
            .build();
        commanderFormat = formatRepository.save(commanderFormat);

        // Create test cards
        lightningBolt = createCard("Lightning Bolt", "Instant", null, false);
        basicMountain = createCard("Mountain", "Land", "Basic", false);
        bannedCard = createCard("Banned Card", "Instant", null, false);
        restrictedCard = createCard("Restricted Card", "Instant", null, false);

        // Create card legality entries
        createCardLegality(bannedCard.getId(), standardFormat.getId(), "banned");
        createCardLegality(restrictedCard.getId(), standardFormat.getId(), "restricted");

        // Create test deck
        testDeck = new DeckEntity();
        testDeck.setName("Test Deck");
        testDeck.setDescription("Test deck for validation");
        testDeck.setFormatId(standardFormat.getId());
        testDeck.setUserId(testUser.getId());
        testDeck.setCreated(LocalDateTime.now());
        testDeck.setModified(LocalDateTime.now());
        testDeck.setIsPrivate(false);
        testDeck = deckRepository.save(testDeck);
    }

    private CardEntity createCard(String name, String cardType, String supertype, boolean unlimited) {
        CardEntity card = new CardEntity();
        card.setName(name);
        card.setManaCost("{R}");
        card.setCmc(1);
        card.setColorIdentity("R");
        card.setTypeLine(cardType + (supertype != null ? " — " + supertype : ""));
        card.setCardType(cardType);
        card.setCardSupertype(supertype);
        card.setRarity("common");
        card.setCardText("Test card text");
        card.setImageUrl("http://example.com/image.jpg");
        card.setLanguage("en");
        card.setCollectorNumber("001");
        card.setUnlimitedCopies(unlimited);
        card.setFoil(false);
        card.setGameChanger(false);
        card.setPromo(false);
        card.setVariation(false);
        return cardRepository.save(card);
    }

    private void createCardLegality(Long cardId, Long formatId, String status) {
        CardLegalityEntity legality = new CardLegalityEntity();
        legality.setCardId(cardId);
        legality.setFormatId(formatId);
        legality.setLegalityStatus(status);
        cardLegalityRepository.save(legality);
    }

    @Test
    @DisplayName("Should setup test data correctly")
    void shouldSetupTestDataCorrectly() {
        // Then - verify all test data was created
        assertThat(testUser.getId()).isNotNull();
        assertThat(standardFormat.getName()).isEqualTo("Standard");
        assertThat(commanderFormat.getName()).isEqualTo("Commander");
        assertThat(lightningBolt.getName()).isEqualTo("Lightning Bolt");
        assertThat(basicMountain.getCardSupertype()).isEqualTo("Basic");
        assertThat(testDeck.getFormatId()).isEqualTo(standardFormat.getId());

        // Verify card legality was created
        var bannedLegality = cardLegalityRepository.findByCardIdAndFormatId(bannedCard.getId(), standardFormat.getId());
        assertThat(bannedLegality).isPresent();
        assertThat(bannedLegality.get().getLegalityStatus()).isEqualTo("banned");

        var restrictedLegality = cardLegalityRepository.findByCardIdAndFormatId(restrictedCard.getId(), standardFormat.getId());
        assertThat(restrictedLegality).isPresent();
        assertThat(restrictedLegality.get().getLegalityStatus()).isEqualTo("restricted");
    }

    @Nested
    @DisplayName("Card Legality Repository Tests")
    class CardLegalityRepositoryTests {

        @Test
        @DisplayName("Should find banned cards correctly")
        void shouldFindBannedCardsCorrectly() {
            // When
            boolean isBanned = cardLegalityRepository.isCardBanned(bannedCard.getId(), standardFormat.getId());
            boolean isNotBanned = cardLegalityRepository.isCardBanned(lightningBolt.getId(), standardFormat.getId());

            // Then
            assertThat(isBanned).isTrue();
            assertThat(isNotBanned).isFalse();
        }

        @Test
        @DisplayName("Should find restricted cards correctly")
        void shouldFindRestrictedCardsCorrectly() {
            // When
            boolean isRestricted = cardLegalityRepository.isCardRestricted(restrictedCard.getId(), standardFormat.getId());
            boolean isNotRestricted = cardLegalityRepository.isCardRestricted(lightningBolt.getId(), standardFormat.getId());

            // Then
            assertThat(isRestricted).isTrue();
            assertThat(isNotRestricted).isFalse();
        }
    }

    @Nested
    @DisplayName("Card In Deck Repository Tests")
    class CardInDeckRepositoryTests {

        @Test
        @DisplayName("Should calculate deck totals correctly")
        void shouldCalculateDeckTotalsCorrectly() {
            // Given - Add some cards to deck
            cardInDeckRepository.save(createCardInDeck(lightningBolt.getId(), 4, "main"));
            cardInDeckRepository.save(createCardInDeck(basicMountain.getId(), 20, "main"));
            cardInDeckRepository.save(createCardInDeck(bannedCard.getId(), 2, "sideboard"));

            // When
            Integer mainDeckTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "main");
            Integer sideboardTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "sideboard");

            // Then
            assertThat(mainDeckTotal).isEqualTo(24); // 4 + 20
            assertThat(sideboardTotal).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate totals excluding specific cards")
        void shouldCalculateTotalsExcludingSpecificCards() {
            // Given
            cardInDeckRepository.save(createCardInDeck(lightningBolt.getId(), 4, "main"));
            cardInDeckRepository.save(createCardInDeck(basicMountain.getId(), 20, "main"));
            cardInDeckRepository.save(createCardInDeck(restrictedCard.getId(), 1, "main"));

            // When - Exclude lightning bolt from total
            Integer totalExcludingLightning = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(
                testDeck.getId(), "main", lightningBolt.getId());

            // Then
            assertThat(totalExcludingLightning).isEqualTo(21); // 20 + 1, excluding 4
        }

        private CardInDeckEntity createCardInDeck(Long cardId, int quantity, String section) {
            return cardInDeckRepository.save(
                CardInDeckEntity.builder()
                    .cardId(cardId)
                    .deckId(testDeck.getId())
                    .quantity(quantity)
                    .section(section)
                    .build()
            );
        }
    }
}
