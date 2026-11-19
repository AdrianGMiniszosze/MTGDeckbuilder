package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.DeckValidationService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;import com.deckbuilder.mtgdeckbuilder.infrastructure.CardLegalityRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckCommanderRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckCommanderEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Deck Validation Repository Integration Tests")
@Transactional
@AutoConfigureMockMvc
class DeckValidationIntegrationTest extends PostgresIntegrationTest {

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

    @Autowired
    private DeckCommanderRepository deckCommanderRepository;

    @Autowired
    private DeckValidationService deckValidationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

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
        testUser.setCreatedAt(LocalDateTime.now());
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
        card.setCopiesLimit(unlimited ? 0 : null);
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

    private DeckEntity createDeck(FormatEntity format) {
        DeckEntity deck = new DeckEntity();
        deck.setName("Deck for " + format.getName());
        deck.setFormatId(format.getId());
        deck.setUserId(testUser.getId());
        deck.setCreated(LocalDateTime.now());
        deck.setModified(LocalDateTime.now());
        deck.setIsPrivate(false);
        return deckRepository.save(deck);
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

    @Nested
    @DisplayName("Deck Evaluation Tests")
    class DeckEvaluationTests {

        @Test
        @DisplayName("Should count copies across main and sideboard")
        void shouldCountCopiesAcrossSections() {
            // Given - 4 in main + 1 in sideboard = 5 copies of the same card
            addCard(lightningBolt.getId(), 4, "main");
            addCard(lightningBolt.getId(), 1, "sideboard");

            // When
            DeckValidationResult result = deckValidationService.evaluate(testDeck.getId());

            // Then
            assertThat(result.getStatus()).isEqualTo(DeckStatus.DRAFT);
            assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.ILLEGAL);
            DeckIssue issue = singleIssue(result, DeckIssueCode.MAX_COPIES_EXCEEDED);
            assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.ERROR);
            assertThat(issue.getCardId()).isEqualTo(lightningBolt.getId());
            assertThat(issue.getMessage()).contains("5 copies").contains("limit of 4");
            // The limit is exceeded by the whole group, not by one row.
            assertThat(issue.getDeckContentId()).isNull();
            assertThat(issue.getSection()).isNull();
        }

        @Test
        @DisplayName("Should count copies by oracle id across printings")
        void shouldCountCopiesByOracleIdAcrossPrintings() {
            // Given - two printings of the same card, 3 + 2 copies
            UUID oracleId = UUID.randomUUID();
            lightningBolt.setOracleId(oracleId);
            cardRepository.save(lightningBolt);
            CardEntity secondPrinting = createCard("Lightning Bolt (Alt)", "Instant", null, false);
            secondPrinting.setOracleId(oracleId);
            cardRepository.save(secondPrinting);

            addCard(lightningBolt.getId(), 3, "main");
            addCard(secondPrinting.getId(), 2, "main");

            // When
            DeckValidationResult result = deckValidationService.evaluate(testDeck.getId());

            // Then
            DeckIssue issue = singleIssue(result, DeckIssueCode.MAX_COPIES_EXCEEDED);
            assertThat(issue.getMessage()).contains("5 copies");
        }

        @Test
        @DisplayName("Should flag a banned card on the row that contains it")
        void shouldFlagBannedCardOnTheRow() {
            // Given
            CardInDeckEntity entry = addCard(bannedCard.getId(), 1, "main");

            // When
            DeckValidationResult result = deckValidationService.evaluate(testDeck.getId());

            // Then
            DeckIssue issue = singleIssue(result, DeckIssueCode.CARD_BANNED);
            assertThat(issue.getCardId()).isEqualTo(bannedCard.getId());
            assertThat(issue.getDeckContentId()).isEqualTo(entry.getId());
            assertThat(issue.getSection()).isEqualTo("main");
        }

        @Test
        @DisplayName("Should ignore the maybeboard for copy limits and legality")
        void shouldIgnoreMaybeboard() {
            // Given - banned card with 10 copies, but only in the maybeboard
            addCard(bannedCard.getId(), 10, "maybeboard");

            // When
            DeckValidationResult result = deckValidationService.evaluate(testDeck.getId());

            // Then
            assertThat(issuesWithCode(result, DeckIssueCode.CARD_BANNED)).isEmpty();
            assertThat(issuesWithCode(result, DeckIssueCode.MAX_COPIES_EXCEEDED)).isEmpty();
        }

        @Test
        @DisplayName("Should not warn about legality when the format does not track it")
        void shouldSkipLegalityWhenFormatDoesNotTrackIt() {
            // Given - casual format without legality tracking and no size rules
            FormatEntity casual = formatRepository.save(FormatEntity.builder()
                .name("Casual Test")
                .description("No format rules")
                .minDeckSize(0)
                .tracksLegality(false)
                .build());
            DeckEntity casualDeck = createDeck(casual);
            cardInDeckRepository.save(CardInDeckEntity.builder()
                .cardId(lightningBolt.getId())
                .deckId(casualDeck.getId())
                .quantity(4)
                .section("main")
                .build());

            // When
            DeckValidationResult result = deckValidationService.evaluate(casualDeck.getId());

            // Then
            assertThat(result.getIssues()).isEmpty();
            assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.LEGAL);
            assertThat(result.getStatus()).isEqualTo(DeckStatus.DRAFT);
        }

        @Test
        @DisplayName("Should report a draft deck below the format minimum")
        void shouldReportDeckTooSmall() {
            // Given
            addCard(lightningBolt.getId(), 4, "main");

            // When
            DeckValidationResult result = deckValidationService.evaluate(testDeck.getId());

            // Then
            DeckIssue issue = singleIssue(result, DeckIssueCode.DECK_TOO_SMALL);
            assertThat(issue.getSection()).isEqualTo("main");
            assertThat(issue.getMessage()).contains("requires at least 60");
        }

        private CardInDeckEntity addCard(Long cardId, int quantity, String section) {
            return DeckValidationIntegrationTest.this.addCard(testDeck.getId(), cardId, quantity, section);
        }

        private DeckIssue singleIssue(DeckValidationResult result, DeckIssueCode code) {
            List<DeckIssue> matching = issuesWithCode(result, code);
            assertThat(matching).hasSize(1);
            return matching.get(0);
        }

        private List<DeckIssue> issuesWithCode(DeckValidationResult result, DeckIssueCode code) {
            return result.getIssues().stream().filter(issue -> issue.getCode() == code).toList();
        }
    }

    @Nested
    @DisplayName("Lifecycle Endpoint Tests")
    class LifecycleEndpointTests {

        @Test
        @DisplayName("Should return 200 with the diagnosis")
        void shouldReturnDiagnosisOverHttp() throws Exception {
            // Given - illegal deck (too small, unknown legality)
            addCard(testDeck.getId(), lightningBolt.getId(), 4, "main");

            // When/Then
            mockMvc.perform(get("/decks/{id}/validation", testDeck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deck_id").value(testDeck.getId()))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.validation_status").value("illegal"))
                .andExpect(jsonPath("$.issues[*].code", hasItem("DECK_TOO_SMALL")));
        }

        @Test
        @DisplayName("Should return 409 DECK_NOT_LEGAL with the issues")
        void shouldReturnConflictWhenPublishingIllegalDeck() throws Exception {
            // Given - illegal deck (too small)
            addCard(testDeck.getId(), lightningBolt.getId(), 4, "main");

            // When/Then
            mockMvc.perform(post("/decks/{id}/publish", testDeck.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DECK_NOT_LEGAL"))
                .andExpect(jsonPath("$.issues[*].code", hasItem("DECK_TOO_SMALL")));
        }

        @Test
        @DisplayName("Should publish and unpublish a legal deck over HTTP")
        void shouldPublishAndUnpublishOverHttp() throws Exception {
            // Given - casual format, no legality or size rules
            FormatEntity casual = formatRepository.save(FormatEntity.builder()
                .name("Casual HTTP")
                .description("No format rules")
                .minDeckSize(0)
                .tracksLegality(false)
                .build());
            DeckEntity deck = createDeck(casual);
            cardInDeckRepository.save(CardInDeckEntity.builder()
                .cardId(lightningBolt.getId())
                .deckId(deck.getId())
                .quantity(4)
                .section("main")
                .build());

            // When/Then - publish
            mockMvc.perform(post("/decks/{id}/publish", deck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deck.getId()))
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.validation_status").value("legal"));

            // When/Then - unpublish
            mockMvc.perform(post("/decks/{id}/unpublish", deck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deck.getId()))
                .andExpect(jsonPath("$.status").value("draft"));
        }

        @Test
        @DisplayName("Should include the diagnosis in the deck detail")
        void shouldIncludeDiagnosisInDeckDetail() throws Exception {
            // Given - illegal deck (too small, unknown legality)
            addCard(testDeck.getId(), lightningBolt.getId(), 4, "main");

            // When/Then
            mockMvc.perform(get("/decks/{id}", testDeck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDeck.getId()))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.validation_status").value("illegal"))
                .andExpect(jsonPath("$.issue_count.errors").value(1))
                .andExpect(jsonPath("$.issues[*].code", hasItem("DECK_TOO_SMALL")));
        }

        @Test
        @DisplayName("Should include the main board of every listed deck")
        void shouldIncludeBoardsInDeckList() throws Exception {
            // Given - a deck with four copies in the main board
            addCard(testDeck.getId(), lightningBolt.getId(), 4, "main");

            // When/Then
            mockMvc.perform(get("/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testDeck.getId().intValue()))
                .andExpect(jsonPath("$[0].main_board[*].card_id", hasItem(lightningBolt.getId().intValue())))
                .andExpect(jsonPath("$[0].main_board[*].quantity", hasItem(4)));
        }

        @Test
        @DisplayName("Should filter the deck list by owner, format and status")
        void shouldFilterDeckList() throws Exception {
            // Given - testDeck belongs to testUser, uses standardFormat and is a draft
            mockMvc.perform(get("/decks").param("user_id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testDeck.getId().intValue()));

            mockMvc.perform(get("/decks").param("format", standardFormat.getId().toString())
                    .param("status", "draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testDeck.getId().intValue()));

            mockMvc.perform(get("/decks").param("status", "published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

            mockMvc.perform(get("/decks").param("format", commanderFormat.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should reject an unknown status filter with 400")
        void shouldRejectUnknownStatusFilter() throws Exception {
            mockMvc.perform(get("/decks").param("status", "archived"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DECK_COMPOSITION"));
        }

        @Test
        @DisplayName("Should save an illegal deck with 201 and its diagnosis")
        void shouldSaveIllegalDeck() throws Exception {
            // Given - an empty Standard deck is illegal but saving is free
            String body = String.format("""
                {"deck_name": "Illegal but saved", "user_id": %d, "format": %d}
                """, testUser.getId(), standardFormat.getId());

            // When/Then
            mockMvc.perform(post("/decks").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.validation_status").value("illegal"))
                .andExpect(jsonPath("$.issues[*].code", hasItem("DECK_TOO_SMALL")));
        }

        @Test
        @DisplayName("Should preserve contents and commanders when the PUT omits them")
        void shouldPreserveRowsWhenPutOmitsThem() throws Exception {
            // Given
            addCard(testDeck.getId(), lightningBolt.getId(), 4, "main");
            deckCommanderRepository.save(new DeckCommanderEntity(testDeck.getId(), lightningBolt.getId()));
            // Commit the setup to the database so the request cannot miss it: with
            // all rows unflushed the orphan-removal bug stayed invisible.
            entityManager.flush();
            entityManager.clear();

            String body = String.format("""
                {"deck_name": "Renamed", "user_id": %d, "format": %d}
                """, testUser.getId(), standardFormat.getId());

            // When/Then - the generated DTO turns an omitted array into [], so the
            // controller has to rely on the recorded JSON fields.
            mockMvc.perform(put("/decks/{id}", testDeck.getId()).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deck_name").value("Renamed"))
                .andExpect(jsonPath("$.commander_card_ids", hasItem(lightningBolt.getId().intValue())));

            // Then - the stored rows survive
            assertThat(cardInDeckRepository.findByDeckId(testDeck.getId())).hasSize(1);
            assertThat(deckCommanderRepository.findByDeckId(testDeck.getId())).hasSize(1);

            mockMvc.perform(get("/decks/{id}", testDeck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.main_board[*].card_id", hasItem(lightningBolt.getId().intValue())))
                .andExpect(jsonPath("$.commander_card_ids", hasItem(lightningBolt.getId().intValue())));
        }

        @Test
        @DisplayName("Should clear contents and commanders when the PUT sends empty arrays")
        void shouldClearRowsWhenPutSendsEmptyArrays() throws Exception {
            // Given
            addCard(testDeck.getId(), lightningBolt.getId(), 4, "main");
            deckCommanderRepository.save(new DeckCommanderEntity(testDeck.getId(), lightningBolt.getId()));
            entityManager.flush();
            entityManager.clear();

            String body = String.format("""
                {"deck_name": "Cleared", "user_id": %d, "format": %d,
                 "deck_contents": [], "commander_card_ids": []}
                """, testUser.getId(), standardFormat.getId());

            // When/Then
            mockMvc.perform(put("/decks/{id}", testDeck.getId()).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deck_name").value("Cleared"))
                .andExpect(jsonPath("$.commander_card_ids").isEmpty());

            assertThat(cardInDeckRepository.findByDeckId(testDeck.getId())).isEmpty();
            assertThat(deckCommanderRepository.findByDeckId(testDeck.getId())).isEmpty();

            mockMvc.perform(get("/decks/{id}", testDeck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.main_board").isEmpty())
                .andExpect(jsonPath("$.commander_card_ids").isEmpty());
        }

        @Test
        @DisplayName("Should reject a deck without a format")
        void shouldRejectDeckWithoutFormat() throws Exception {
            // Given
            String body = String.format("""
                {"deck_name": "No format", "user_id": %d}
                """, testUser.getId());

            // When/Then
            mockMvc.perform(post("/decks").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should add and remove cards over HTTP")
        void shouldAddAndRemoveCardsOverHttp() throws Exception {
            // Given
            String body = String.format("""
                {"card_id": %d, "quantity": 4, "section": "main"}
                """, lightningBolt.getId());

            // When/Then - add
            mockMvc.perform(post("/decks/{id}/cards", testDeck.getId())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.validation_status").value("illegal"))
                .andExpect(jsonPath("$.deck_contents[*].card_id", hasItem(lightningBolt.getId().intValue())))
                .andExpect(jsonPath("$.deck_contents[*].quantity", hasItem(4)));

            // When/Then - remove
            mockMvc.perform(delete("/decks/{id}/cards/{cardId}", testDeck.getId(), lightningBolt.getId())
                    .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deck_contents[*].quantity", hasItem(2)));
        }

        @Test
        @DisplayName("Should return a published deck to draft when a card is added")
        void shouldReturnPublishedDeckToDraftWhenAddingCard() throws Exception {
            // Given - casual format so it can be published
            FormatEntity casual = formatRepository.save(FormatEntity.builder()
                .name("Casual Edit")
                .description("No format rules")
                .minDeckSize(0)
                .tracksLegality(false)
                .build());
            DeckEntity deck = createDeck(casual);
            mockMvc.perform(post("/decks/{id}/publish", deck.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));

            String body = String.format("""
                {"card_id": %d, "quantity": 1}
                """, lightningBolt.getId());

            // When/Then
            mockMvc.perform(post("/decks/{id}/cards", deck.getId())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"));

            DeckEntity stored = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo("draft");
            assertThat(stored.getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("Should return 404 when adding an unknown card")
        void shouldReturn404WhenAddingUnknownCard() throws Exception {
            // Given
            String body = """
                {"card_id": 999999, "quantity": 1}
                """;

            // When/Then
            mockMvc.perform(post("/decks/{id}/cards", testDeck.getId())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CARD_NOT_FOUND"));
        }
    }

    @Test
    @DisplayName("Should read the commander from deck_commander")
    void shouldReadCommanderFromDeckCommander() {
        // Given - a format that requires a commander and a legendary creature
        FormatEntity commanderRules = formatRepository.save(FormatEntity.builder()
            .name("Commander Rules Test")
            .description("Commander validation")
            .minDeckSize(0)
            .maxCopies(1)
            .singleton(true)
            .requiresCommander(true)
            .tracksLegality(false)
            .build());
        CardEntity legend = createCard("Test Legend", "Creature", "Human", false);
        legend.setTypeLine("Legendary Creature — Human");
        legend = cardRepository.save(legend);

        DeckEntity deck = createDeck(commanderRules);
        addCard(deck.getId(), legend.getId(), 1, "main");
        deckCommanderRepository.save(new DeckCommanderEntity(deck.getId(), legend.getId()));

        // When/Then - declared commander
        assertThat(deckValidationService.evaluate(deck.getId()).getIssues()).isEmpty();

        // When/Then - no commander row
        deckCommanderRepository.deleteByDeckId(deck.getId());
        deckCommanderRepository.flush();

        assertThat(deckValidationService.evaluate(deck.getId()).getIssues())
            .extracting(DeckIssue::getCode)
            .containsExactly(DeckIssueCode.MISSING_COMMANDER);
    }

    private CardInDeckEntity addCard(Long deckId, Long cardId, int quantity, String section) {
        return cardInDeckRepository.save(CardInDeckEntity.builder()
            .cardId(cardId)
            .deckId(deckId)
            .quantity(quantity)
            .section(section)
            .build());
    }
}
