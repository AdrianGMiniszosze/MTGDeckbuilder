package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Card In Deck Repository Extended Tests")
class CardInDeckRepositoryExtendedTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CardInDeckRepository cardInDeckRepository;

    private DeckEntity testDeck;
    private CardEntity card1, card2, card3;

    @BeforeEach
    void setUp() {
        // Create test user with all required fields
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setHashedPassword("hashedpassword123");
        user.setCountry("US");
        user.setRegistrationDate(LocalDateTime.now());
        entityManager.persist(user);

        // Create test format
        FormatEntity format = FormatEntity.builder()
            .name("Standard")
            .description("Standard format")
            .minDeckSize(60)
            .maxDeckSize(60)
            .maxSideboardSize(15)
            .build();
        entityManager.persist(format);

        // Create test deck
        testDeck = new DeckEntity();
        testDeck.setName("Test Deck");
        testDeck.setDescription("Test deck for repository tests");
        testDeck.setFormatId(format.getId());
        testDeck.setUserId(user.getId());
        testDeck.setCreated(LocalDateTime.now());
        testDeck.setModified(LocalDateTime.now());
        testDeck.setIsPrivate(false);
        entityManager.persist(testDeck);

        // Create test cards
        card1 = createCard("Card 1");
        card2 = createCard("Card 2");
        card3 = createCard("Card 3");

        entityManager.flush();
    }

    private CardEntity createCard(String name) {
        CardEntity card = new CardEntity();
        card.setName(name);
        card.setManaCost("{1}");
        card.setCmc(1);
        card.setColorIdentity("C");
        card.setTypeLine("Artifact");
        card.setCardType("Artifact");
        card.setRarity("common");
        card.setCardText("Test card text");
        card.setImageUrl("http://example.com/image.jpg");
        card.setLanguage("en");
        card.setCollectorNumber("001");
        card.setUnlimitedCopies(false);
        card.setFoil(false);
        card.setGameChanger(false);
        card.setPromo(false);
        card.setVariation(false);
        entityManager.persist(card);
        return card;
    }

    private void addCardToDeck(CardEntity card, String section, int quantity) {
        CardInDeckEntity cardInDeck = CardInDeckEntity.builder()
            .cardId(card.getId())
            .deckId(testDeck.getId())
            .quantity(quantity)
            .section(section)
            .build();
        entityManager.persist(cardInDeck);
    }

    @Test
    @DisplayName("Should sum quantities excluding specified card")
    void shouldSumQuantitiesExcludingSpecifiedCard() {
        // Given
        addCardToDeck(card1, "main", 4);
        addCardToDeck(card2, "main", 3);
        addCardToDeck(card3, "main", 2);
        entityManager.flush();

        // When
        Integer total = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(
            testDeck.getId(), "main", card2.getId());

        // Then
        assertThat(total).isEqualTo(6); // 4 + 2, excluding card2's 3
    }

    @Test
    @DisplayName("Should return 0 when no cards match criteria excluding specified card")
    void shouldReturnZeroWhenNoCardsMatchCriteriaExcludingSpecifiedCard() {
        // Given
        addCardToDeck(card1, "main", 4);
        entityManager.flush();

        // When
        Integer total = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(
            testDeck.getId(), "main", card1.getId());

        // Then
        assertThat(total).isEqualTo(0);
    }

    @Test
    @DisplayName("Should sum quantities by deck and section")
    void shouldSumQuantitiesByDeckAndSection() {
        // Given
        addCardToDeck(card1, "main", 4);
        addCardToDeck(card2, "main", 3);
        addCardToDeck(card3, "main", 2);
        addCardToDeck(card1, "sideboard", 2); // Different section, should not be included
        entityManager.flush();

        // When
        Integer total = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "main");

        // Then
        assertThat(total).isEqualTo(9); // 4 + 3 + 2
    }

    @Test
    @DisplayName("Should return 0 when no cards in section")
    void shouldReturnZeroWhenNoCardsInSection() {
        // When
        Integer total = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "main");

        // Then
        assertThat(total).isEqualTo(0);
    }

    @Test
    @DisplayName("Should differentiate between sections")
    void shouldDifferentiateBetweenSections() {
        // Given
        addCardToDeck(card1, "main", 4);
        addCardToDeck(card2, "sideboard", 3);
        addCardToDeck(card3, "maybeboard", 2);
        entityManager.flush();

        // When
        Integer mainTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "main");
        Integer sideboardTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "sideboard");
        Integer maybeboardTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "maybeboard");

        // Then
        assertThat(mainTotal).isEqualTo(4);
        assertThat(sideboardTotal).isEqualTo(3);
        assertThat(maybeboardTotal).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle multiple cards of same type in section")
    void shouldHandleMultipleCardsOfSameTypeInSection() {
        // Given
        addCardToDeck(card1, "main", 4);
        addCardToDeck(card1, "sideboard", 2);
        addCardToDeck(card2, "main", 3);
        entityManager.flush();

        // When
        Integer mainTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "main");
        Integer sideboardTotal = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "sideboard");

        // Then
        assertThat(mainTotal).isEqualTo(7); // 4 + 3
        assertThat(sideboardTotal).isEqualTo(2);
    }

    @Test
    @DisplayName("Should exclude correct card when multiple cards in section")
    void shouldExcludeCorrectCardWhenMultipleCardsInSection() {
        // Given
        addCardToDeck(card1, "main", 4);
        addCardToDeck(card2, "main", 3);
        addCardToDeck(card3, "main", 2);
        addCardToDeck(card1, "sideboard", 1); // Different section, should not affect calculation
        entityManager.flush();

        // When
        Integer totalExcludingCard1 = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(
            testDeck.getId(), "main", card1.getId());
        Integer totalExcludingCard2 = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(
            testDeck.getId(), "main", card2.getId());

        // Then
        assertThat(totalExcludingCard1).isEqualTo(5); // 3 + 2, excluding card1's 4
        assertThat(totalExcludingCard2).isEqualTo(6); // 4 + 2, excluding card2's 3
    }

    @Test
    @DisplayName("Should return null as 0 when using COALESCE")
    void shouldReturnNullAsZeroWhenUsingCoalesce() {
        // When - Query empty deck
        Integer total = cardInDeckRepository.sumQuantityByDeckIdAndSection(testDeck.getId(), "main");
        Integer totalExcluding = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(
            testDeck.getId(), "main", 999L);

        // Then - Should return 0, not null
        assertThat(total).isEqualTo(0);
        assertThat(totalExcluding).isEqualTo(0);
    }
}
