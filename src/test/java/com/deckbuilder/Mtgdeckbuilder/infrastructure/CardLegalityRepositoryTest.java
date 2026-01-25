package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Card Legality Repository Tests")
class CardLegalityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CardLegalityRepository cardLegalityRepository;

    private CardEntity testCard;
    private FormatEntity standardFormat;
    private FormatEntity commanderFormat;

    @BeforeEach
    void setUp() {
        // Create test card
        testCard = new CardEntity();
        testCard.setName("Lightning Bolt");
        testCard.setManaCost("{R}");
        testCard.setCmc(1);
        testCard.setColorIdentity("R");
        testCard.setTypeLine("Instant");
        testCard.setCardType("Instant");
        testCard.setRarity("common");
        testCard.setCardText("Lightning Bolt deals 3 damage to any target.");
        testCard.setImageUrl("http://example.com/image.jpg");
        testCard.setLanguage("en");
        testCard.setCollectorNumber("001");
        testCard.setUnlimitedCopies(false);
        testCard.setFoil(false);
        testCard.setGameChanger(false);
        testCard.setPromo(false);
        testCard.setVariation(false);
        entityManager.persist(testCard);

        // Create test formats
        standardFormat = FormatEntity.builder()
            .name("Standard")
            .description("Standard format")
            .minDeckSize(60)
            .maxDeckSize(60)
            .maxSideboardSize(15)
            .build();
        entityManager.persist(standardFormat);

        commanderFormat = FormatEntity.builder()
            .name("Commander")
            .description("Commander format")
            .minDeckSize(100)
            .maxDeckSize(100)
            .maxSideboardSize(0)
            .build();
        entityManager.persist(commanderFormat);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should find card legality by card ID and format ID")
    void shouldFindCardLegalityByCardIdAndFormatId() {
        // Given
        CardLegalityEntity legality = new CardLegalityEntity();
        legality.setCardId(testCard.getId());
        legality.setFormatId(standardFormat.getId());
        legality.setLegalityStatus("legal");
        entityManager.persist(legality);
        entityManager.flush();

        // When
        Optional<CardLegalityEntity> result = cardLegalityRepository
            .findByCardIdAndFormatId(testCard.getId(), standardFormat.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLegalityStatus()).isEqualTo("legal");
        assertThat(result.get().getCardId()).isEqualTo(testCard.getId());
        assertThat(result.get().getFormatId()).isEqualTo(standardFormat.getId());
    }

    @Test
    @DisplayName("Should return empty when card legality not found")
    void shouldReturnEmptyWhenCardLegalityNotFound() {
        // When
        Optional<CardLegalityEntity> result = cardLegalityRepository
            .findByCardIdAndFormatId(999L, 999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if card is banned")
    void shouldCheckIfCardIsBanned() {
        // Given
        CardLegalityEntity bannedLegality = new CardLegalityEntity();
        bannedLegality.setCardId(testCard.getId());
        bannedLegality.setFormatId(standardFormat.getId());
        bannedLegality.setLegalityStatus("banned");
        entityManager.persist(bannedLegality);
        entityManager.flush();

        // When
        boolean isBanned = cardLegalityRepository.isCardBanned(testCard.getId(), standardFormat.getId());

        // Then
        assertThat(isBanned).isTrue();
    }

    @Test
    @DisplayName("Should return false when card is not banned")
    void shouldReturnFalseWhenCardIsNotBanned() {
        // Given
        CardLegalityEntity legalLegality = new CardLegalityEntity();
        legalLegality.setCardId(testCard.getId());
        legalLegality.setFormatId(standardFormat.getId());
        legalLegality.setLegalityStatus("legal");
        entityManager.persist(legalLegality);
        entityManager.flush();

        // When
        boolean isBanned = cardLegalityRepository.isCardBanned(testCard.getId(), standardFormat.getId());

        // Then
        assertThat(isBanned).isFalse();
    }

    @Test
    @DisplayName("Should check if card is restricted")
    void shouldCheckIfCardIsRestricted() {
        // Given
        CardLegalityEntity restrictedLegality = new CardLegalityEntity();
        restrictedLegality.setCardId(testCard.getId());
        restrictedLegality.setFormatId(standardFormat.getId());
        restrictedLegality.setLegalityStatus("restricted");
        entityManager.persist(restrictedLegality);
        entityManager.flush();

        // When
        boolean isRestricted = cardLegalityRepository.isCardRestricted(testCard.getId(), standardFormat.getId());

        // Then
        assertThat(isRestricted).isTrue();
    }

    @Test
    @DisplayName("Should return false when card is not restricted")
    void shouldReturnFalseWhenCardIsNotRestricted() {
        // Given
        CardLegalityEntity legalLegality = new CardLegalityEntity();
        legalLegality.setCardId(testCard.getId());
        legalLegality.setFormatId(standardFormat.getId());
        legalLegality.setLegalityStatus("legal");
        entityManager.persist(legalLegality);
        entityManager.flush();

        // When
        boolean isRestricted = cardLegalityRepository.isCardRestricted(testCard.getId(), standardFormat.getId());

        // Then
        assertThat(isRestricted).isFalse();
    }

    @Test
    @DisplayName("Should return false for non-existent card legality")
    void shouldReturnFalseForNonExistentCardLegality() {
        // When
        boolean isBanned = cardLegalityRepository.isCardBanned(999L, 999L);
        boolean isRestricted = cardLegalityRepository.isCardRestricted(999L, 999L);

        // Then
        assertThat(isBanned).isFalse();
        assertThat(isRestricted).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple legality statuses for same card in different formats")
    void shouldHandleMultipleLegalityStatusesForSameCard() {
        // Given
        CardLegalityEntity standardLegality = new CardLegalityEntity();
        standardLegality.setCardId(testCard.getId());
        standardLegality.setFormatId(standardFormat.getId());
        standardLegality.setLegalityStatus("legal");
        entityManager.persist(standardLegality);

        CardLegalityEntity commanderLegality = new CardLegalityEntity();
        commanderLegality.setCardId(testCard.getId());
        commanderLegality.setFormatId(commanderFormat.getId());
        commanderLegality.setLegalityStatus("banned");
        entityManager.persist(commanderLegality);

        entityManager.flush();

        // When
        boolean isBannedInStandard = cardLegalityRepository.isCardBanned(testCard.getId(), standardFormat.getId());
        boolean isBannedInCommander = cardLegalityRepository.isCardBanned(testCard.getId(), commanderFormat.getId());

        // Then
        assertThat(isBannedInStandard).isFalse();
        assertThat(isBannedInCommander).isTrue();
    }
}
