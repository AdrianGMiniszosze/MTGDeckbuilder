package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.DeckValidationServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardLegalityRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.FormatNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Validation Service Tests")
class DeckValidationServiceTest {

	private static final long DECK_ID = 1L;
	private static final long FORMAT_ID = 1L;

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

	private DeckEntity deck;

	@BeforeEach
	void setUp() {
		this.deck = new DeckEntity();
		this.deck.setId(DECK_ID);
		this.deck.setFormatId(FORMAT_ID);
		this.deck.setStatus(DeckStatus.DRAFT.getValue());
	}

	// ==================== LEGALITY ====================

	@Test
	@DisplayName("Should report a legal deck when every rule passes")
	void shouldReportLegalDeck() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 4, "main")), List.of(bolt),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getDeckId()).isEqualTo(DECK_ID);
		assertThat(result.getStatus()).isEqualTo(DeckStatus.DRAFT);
		assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.LEGAL);
		assertThat(result.getIssues()).isEmpty();
		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	@DisplayName("Should flag a banned card")
	void shouldFlagBannedCard() {
		final CardEntity lotus = card(1L, "Black Lotus", "Artifact");
		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(lotus),
				List.of(legality(1L, FORMAT_ID, "banned")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.ILLEGAL);
		final DeckIssue issue = singleIssue(result);
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.CARD_BANNED);
		assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.ERROR);
		assertThat(issue.getCardId()).isEqualTo(1L);
		assertThat(issue.getDeckContentId()).isEqualTo(10L);
		assertThat(issue.getSection()).isEqualTo("main");
	}

	@Test
	@DisplayName("Should flag a card that is not legal in the format")
	void shouldFlagNotLegalCard() {
		final CardEntity card = card(1L, "Modern Only", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(card),
				List.of(legality(1L, FORMAT_ID, "not_legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(singleIssue(result).getCode()).isEqualTo(DeckIssueCode.CARD_NOT_LEGAL);
	}

	@Test
	@DisplayName("Should warn when the format tracks legality but the card has no row")
	void shouldWarnUnknownLegality() {
		final CardEntity card = card(1L, "Unimported Card", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(card), List.of());

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		final DeckIssue issue = singleIssue(result);
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.LEGALITY_UNKNOWN);
		assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.WARNING);
		assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.UNKNOWN);
		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	@DisplayName("Should not warn about legality when the format does not track it")
	void shouldSkipLegalityWhenFormatDoesNotTrackIt() {
		final CardEntity card = card(1L, "Anything", "Instant");
		given(format(FORMAT_ID, 0, null, 15, 4, false, false), List.of(entry(10L, 1L, 1, "main")),
				List.of(card), List.of());

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
		assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.LEGAL);
	}

	// ==================== COPY LIMITS ====================

	@Test
	@DisplayName("Should count copies across main and sideboard")
	void shouldCountCopiesAcrossSections() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 4, "main"), entry(11L, 1L, 1, "sideboard")), List.of(bolt),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		final DeckIssue issue = singleIssue(result);
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.MAX_COPIES_EXCEEDED);
		assertThat(issue.getMessage()).contains("5 copies").contains("4");
		assertThat(issue.getDeckContentId()).isNull();
		assertThat(issue.getSection()).isNull();
	}

	@Test
	@DisplayName("Should count copies by oracle id across printings")
	void shouldCountCopiesByOracleId() {
		final UUID oracleId = UUID.randomUUID();
		final CardEntity first = card(1L, "Lightning Bolt", "Instant");
		first.setOracleId(oracleId);
		final CardEntity second = card(2L, "Lightning Bolt", "Instant");
		second.setOracleId(oracleId);

		given(baseFormat(), List.of(entry(10L, 1L, 3, "main"), entry(11L, 2L, 2, "main")), List.of(first, second),
				List.of(legality(1L, FORMAT_ID, "legal"), legality(2L, FORMAT_ID, "legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		final DeckIssue issue = singleIssue(result);
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.MAX_COPIES_EXCEEDED);
		assertThat(issue.getMessage()).contains("5 copies");
	}

	@Test
	@DisplayName("Should allow any number of copies when copies_limit is zero")
	void shouldAllowUnlimitedCopiesWhenCardSaysSo() {
		final CardEntity petitioners = card(1L, "Persistent Petitioners", "Creature");
		petitioners.setCopiesLimit(0);
		given(baseFormat(), List.of(entry(10L, 1L, 20, "main")), List.of(petitioners),
				List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should allow up to the exact copies_limit the card sets")
	void shouldAllowExactCopiesLimit() {
		final CardEntity dwarves = card(1L, "Seven Dwarves", "Creature");
		dwarves.setCopiesLimit(7);
		given(baseFormat(), List.of(entry(10L, 1L, 7, "main")), List.of(dwarves),
				List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should flag when the exact copies_limit is exceeded")
	void shouldFlagExactCopiesLimitExceeded() {
		final CardEntity dwarves = card(1L, "Seven Dwarves", "Creature");
		dwarves.setCopiesLimit(7);
		given(baseFormat(), List.of(entry(10L, 1L, 8, "main")), List.of(dwarves),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.MAX_COPIES_EXCEEDED);
		assertThat(issue.getMessage()).contains("limit of 7");
	}

	@Test
	@DisplayName("Should exempt basic lands from the copy limit")
	void shouldExemptBasicLands() {
		final CardEntity mountain = card(1L, "Mountain", "Land");
		mountain.setIsBasicLand(true);
		given(baseFormat(), List.of(entry(10L, 1L, 20, "main")), List.of(mountain),
				List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should report a singleton violation instead of a copy-limit one")
	void shouldReportSingletonViolation() {
		final CardEntity card = card(1L, "Sol Ring", "Artifact");
		given(format(FORMAT_ID, 0, null, null, 4, true, true), List.of(entry(10L, 1L, 2, "main")), List.of(card),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.SINGLETON_VIOLATION);
	}

	@Test
	@DisplayName("Should restrict a card marked as restricted to one copy")
	void shouldRestrictRestrictedCards() {
		final CardEntity recall = card(1L, "Ancestral Recall", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 2, "main")), List.of(recall),
				List.of(legality(1L, FORMAT_ID, "restricted")));

		assertThat(singleIssue(this.deckValidationService.evaluate(DECK_ID)).getCode())
				.isEqualTo(DeckIssueCode.MAX_COPIES_EXCEEDED);
	}

	@Test
	@DisplayName("Should ignore the maybeboard for copy limits and sizes")
	void shouldIgnoreMaybeboard() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		given(format(FORMAT_ID, 0, 60, 15, 4, false, true), List.of(entry(10L, 1L, 100, "maybeboard")),
				List.of(bolt), List.of());

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
	}

	// ==================== SIZES ====================

	@Test
	@DisplayName("Should report a main deck below the format minimum")
	void shouldReportDeckTooSmall() {
		final CardEntity card = card(1L, "Filler", "Instant");
		given(format(FORMAT_ID, 60, null, 15, 4, false, true), List.of(entry(10L, 1L, 4, "main")), List.of(card),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.DECK_TOO_SMALL);
		assertThat(issue.getSection()).isEqualTo("main");
	}

	@Test
	@DisplayName("Should report a main deck above the format maximum")
	void shouldReportDeckTooLarge() {
		final CardEntity mountain = card(1L, "Mountain", "Land");
		mountain.setIsBasicLand(true);
		given(format(FORMAT_ID, 0, 60, 15, 4, false, true), List.of(entry(10L, 1L, 61, "main")),
				List.of(mountain), List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(singleIssue(this.deckValidationService.evaluate(DECK_ID)).getCode())
				.isEqualTo(DeckIssueCode.DECK_TOO_LARGE);
	}

	@Test
	@DisplayName("Should report a sideboard above the format maximum")
	void shouldReportSideboardTooLarge() {
		final CardEntity mountain = card(1L, "Mountain", "Land");
		mountain.setIsBasicLand(true);
		given(format(FORMAT_ID, 0, null, 15, 4, false, true), List.of(entry(10L, 1L, 16, "sideboard")),
				List.of(mountain), List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.SIDEBOARD_TOO_LARGE);
		assertThat(issue.getSection()).isEqualTo("sideboard");
	}

	// ==================== COMPANIONS ====================

	@Test
	@DisplayName("Should report a per-card companion violation")
	void shouldReportCompanionPerCardRule() {
		this.deck.setCompanionCardId(99L);
		final CardEntity lurrus = card(99L, "Lurrus of the Dream-Den", "Creature");
		final CardEntity permanent = card(1L, "Expensive Permanent", "Artifact");
		permanent.setCmc(3);

		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(lurrus, permanent),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.COMPANION_RULE_VIOLATION);
		assertThat(issue.getCardId()).isEqualTo(1L);
		assertThat(issue.getMessage()).contains("Lurrus");
	}

	@Test
	@DisplayName("Should enforce Yorion's extra twenty cards")
	void shouldEnforceYorionDeckSize() {
		this.deck.setCompanionCardId(99L);
		final CardEntity yorion = card(99L, "Yorion, Sky Nomad", "Creature");
		final CardEntity mountain = card(1L, "Mountain", "Land");
		mountain.setIsBasicLand(true);

		given(format(FORMAT_ID, 60, null, null, 4, false, true), List.of(entry(10L, 1L, 79, "main")),
				List.of(yorion, mountain), List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.COMPANION_RULE_VIOLATION);
		assertThat(issue.getMessage()).contains("Yorion").contains("80");
	}

	@Test
	@DisplayName("Should enforce Umori's single nonland type")
	void shouldEnforceUmoriDeckType() {
		this.deck.setCompanionCardId(99L);
		final CardEntity umori = card(99L, "Umori, the Collector", "Creature");
		final CardEntity instant = card(1L, "Instant", "Instant");
		final CardEntity artifact = card(2L, "Artifact", "Artifact");

		given(baseFormat(), List.of(entry(10L, 1L, 1, "main"), entry(11L, 2L, 1, "main")),
				List.of(umori, instant, artifact),
				List.of(legality(1L, FORMAT_ID, "legal"), legality(2L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.COMPANION_RULE_VIOLATION);
		assertThat(issue.getMessage()).contains("Umori");
	}

	@Test
	@DisplayName("Should enforce Zirda's activated-ability requirement")
	void shouldEnforceZirdaActivatedAbility() {
		this.deck.setCompanionCardId(99L);
		final CardEntity zirda = card(99L, "Zirda, the Dawnwaker", "Creature");
		final CardEntity artifact = card(1L, "Vanilla Artifact", "Artifact");
		artifact.setCardText("This artifact does nothing.");

		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(zirda, artifact),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckIssue issue = singleIssue(this.deckValidationService.evaluate(DECK_ID));
		assertThat(issue.getCode()).isEqualTo(DeckIssueCode.COMPANION_RULE_VIOLATION);
		assertThat(issue.getMessage()).contains("Zirda");
	}

	@Test
	@DisplayName("Should not apply companion rules when the deck has no companion")
	void shouldSkipCompanionRulesWithoutCompanion() {
		final CardEntity card = card(1L, "Anything", "Artifact");
		card.setCmc(5);

		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(card),
				List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
		verify(this.cardRepository, never()).findById(99L);
	}

	// ==================== ERRORS AND QUERIES ====================

	@Test
	@DisplayName("Should throw when the deck does not exist")
	void shouldThrowWhenDeckDoesNotExist() {
		when(this.deckRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.deckValidationService.evaluate(9L))
				.isInstanceOf(DeckNotFoundException.class);
	}

	@Test
	@DisplayName("Should throw when the deck's format does not exist")
	void shouldThrowWhenFormatDoesNotExist() {
		when(this.deckRepository.findById(DECK_ID)).thenReturn(Optional.of(this.deck));
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.deckValidationService.evaluate(DECK_ID))
				.isInstanceOf(FormatNotFoundException.class);
	}

	@Test
	@DisplayName("Should return unlimited for basic lands")
	void shouldReturnUnlimitedForBasicLands() {
		final CardEntity mountain = card(1L, "Mountain", "Land");
		mountain.setIsBasicLand(true);
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(mountain));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID)).thenReturn(Optional.empty());

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(9999);
	}

	@Test
	@DisplayName("Should return the card's own copies_limit")
	void shouldReturnCardCopiesLimit() {
		final CardEntity dwarves = card(1L, "Seven Dwarves", "Creature");
		dwarves.setCopiesLimit(7);
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(dwarves));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID)).thenReturn(Optional.empty());

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(7);
	}

	@Test
	@DisplayName("Should fall back to the format copy limit")
	void shouldReturnFormatCopyLimit() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(bolt));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.of(baseFormat()));

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(4);
	}

	@Test
	@DisplayName("Should return the format's section size")
	void shouldReturnSectionSize() {
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.of(format(FORMAT_ID, 60, 80, 15, 4,
				false, true)));

		assertThat(this.deckValidationService.getMaxDeckSize(FORMAT_ID, "main")).isEqualTo(80);
		assertThat(this.deckValidationService.getMaxDeckSize(FORMAT_ID, "sideboard")).isEqualTo(15);
	}

	// ==================== REMOVAL INTEGRITY ====================

	@Test
	@DisplayName("Should reject a removal quantity of zero")
	void shouldRejectZeroRemoval() {
		assertThatThrownBy(() -> this.deckValidationService.validateCardRemoval(DECK_ID, 1L, 0, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("greater than 0");
	}

	@Test
	@DisplayName("Should reject removing a card that is not in the section")
	void shouldRejectRemovalOfMissingCard() {
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(DECK_ID, 1L, "main"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.deckValidationService.validateCardRemoval(DECK_ID, 1L, 1, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Card not found in deck section");
	}

	@Test
	@DisplayName("Should reject removing more copies than available")
	void shouldRejectRemovalAboveAvailable() {
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(DECK_ID, 1L, "main"))
				.thenReturn(Optional.of(entry(10L, 1L, 2, "main")));

		assertThatThrownBy(() -> this.deckValidationService.validateCardRemoval(DECK_ID, 1L, 3, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Cannot remove 3 cards, only 2 available");
	}

	// ==================== EDGE CASES ====================

	@Test
	@DisplayName("Should throw when the deck has no format")
	void shouldThrowWhenDeckHasNoFormat() {
		this.deck.setFormatId(null);
		when(this.deckRepository.findById(DECK_ID)).thenReturn(Optional.of(this.deck));

		assertThatThrownBy(() -> this.deckValidationService.evaluate(DECK_ID))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Deck format is not specified");
	}

	@Test
	@DisplayName("Should ignore rows whose card is no longer loaded")
	void shouldIgnoreEntriesWithoutCard() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 4, "main"), entry(11L, 90L, 4, "main")), List.of(bolt),
				List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
		assertThat(result.getValidationStatus()).isEqualTo(ValidationStatus.LEGAL);
	}

	@Test
	@DisplayName("Should keep the first legality row when the database returns duplicates")
	void shouldKeepFirstLegalityRow() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(bolt),
				List.of(legality(1L, FORMAT_ID, "legal"), legality(1L, FORMAT_ID, "banned")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should skip companion rules when the companion card is missing")
	void shouldSkipMissingCompanion() {
		this.deck.setCompanionCardId(99L);
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		when(this.deckRepository.findById(DECK_ID)).thenReturn(Optional.of(this.deck));
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.of(baseFormat()));
		when(this.cardInDeckRepository.findByDeckId(DECK_ID)).thenReturn(List.of(entry(10L, 1L, 1, "main")));
		when(this.cardRepository.findAllById(any())).thenReturn(List.of(bolt));
		when(this.cardLegalityRepository.findByFormatIdAndCardIdIn(eq(FORMAT_ID), any()))
				.thenReturn(List.of(legality(1L, FORMAT_ID, "legal")));
		when(this.cardRepository.findById(99L)).thenReturn(Optional.empty());

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should ignore companion rows whose card is no longer loaded")
	void shouldIgnoreCompanionEntriesWithoutCard() {
		this.deck.setCompanionCardId(99L);
		final CardEntity companion = card(99L, "Test Companion", "Creature");
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		given(baseFormat(), List.of(entry(10L, 1L, 1, "main"), entry(11L, 90L, 1, "main")),
				List.of(companion, bolt), List.of(legality(1L, FORMAT_ID, "legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should ignore lands and typeless cards when grouping Umori types")
	void shouldIgnoreLandsUnderUmori() {
		this.deck.setCompanionCardId(99L);
		final CardEntity umori = card(99L, "Umori, the Collector", "Creature");
		final CardEntity beast = card(1L, "Beast", "Creature");
		final CardEntity forest = card(2L, "Forest", "Land");
		forest.setIsBasicLand(true);
		final CardEntity typeless = card(3L, "Typeless", null);
		typeless.setCardType(null);

		given(baseFormat(),
				List.of(entry(10L, 1L, 1, "main"), entry(11L, 2L, 1, "main"), entry(12L, 3L, 1, "main"),
						entry(13L, 90L, 1, "main")),
				List.of(umori, beast, forest, typeless),
				List.of(legality(1L, FORMAT_ID, "legal"), legality(2L, FORMAT_ID, "legal"),
						legality(3L, FORMAT_ID, "legal")));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should accept every activated-ability shape Zirda recognises")
	void shouldAcceptActivatedAbilities() {
		this.deck.setCompanionCardId(99L);
		final CardEntity zirda = card(99L, "Zirda, the Dawnwaker", "Creature");
		final CardEntity manaRock = card(1L, "Mana Rock", "Artifact");
		manaRock.setCardText("{T}: Add {G}.");
		final CardEntity tapAbility = card(2L, "Tap Ability", "Creature");
		tapAbility.setCardText("Tap: Draw a card.");
		final CardEntity numericCost = card(3L, "Numeric Cost", "Artifact");
		numericCost.setCardText("2: Draw a card.");
		final CardEntity sacOutlet = card(4L, "Sac Outlet", "Creature");
		sacOutlet.setCardText("Sacrifice this creature: Add {B}.");
		final CardEntity discardOutlet = card(5L, "Discard Outlet", "Creature");
		discardOutlet.setCardText("Discard a card: Draw a card.");
		final CardEntity exileOutlet = card(6L, "Exile Outlet", "Artifact");
		exileOutlet.setCardText("Exile a card from your graveyard: Draw a card.");
		final CardEntity returnOutlet = card(7L, "Return Outlet", "Creature");
		returnOutlet.setCardText("Return this creature to its owner's hand: Draw a card.");
		final CardEntity payOutlet = card(8L, "Pay Outlet", "Creature");
		payOutlet.setCardText("Pay life: Draw a card.");
		final CardEntity removeOutlet = card(9L, "Remove Outlet", "Artifact");
		removeOutlet.setCardText("Remove a counter: Draw a card.");
		final CardEntity untapOutlet = card(10L, "Untap Outlet", "Creature");
		untapOutlet.setCardText("Untap this creature: Draw a card.");
		final CardEntity counterspell = card(11L, "Counterspell", "Instant");
		counterspell.setCardText("Counter target spell.");

		given(baseFormat(),
				List.of(entry(20L, 1L, 1, "main"), entry(21L, 2L, 1, "main"), entry(22L, 3L, 1, "main"),
						entry(23L, 4L, 1, "main"), entry(24L, 5L, 1, "main"), entry(25L, 6L, 1, "main"),
						entry(26L, 7L, 1, "main"), entry(27L, 8L, 1, "main"), entry(28L, 9L, 1, "main"),
						entry(29L, 10L, 1, "main"), entry(30L, 11L, 1, "main"), entry(31L, 90L, 1, "main")),
				List.of(zirda, manaRock, tapAbility, numericCost, sacOutlet, discardOutlet, exileOutlet, returnOutlet,
						payOutlet, removeOutlet, untapOutlet, counterspell),
				legalitiesFor(11));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should flag permanents without an activated ability")
	void shouldFlagPermanentsWithoutActivatedAbility() {
		this.deck.setCompanionCardId(99L);
		final CardEntity zirda = card(99L, "Zirda, the Dawnwaker", "Creature");
		final CardEntity vanilla = card(1L, "Vanilla", "Artifact");
		vanilla.setCardText("This artifact does nothing.");
		final CardEntity noText = card(2L, "No Text", "Creature");
		noText.setCardText(null);
		final CardEntity colonAtStart = card(3L, "Colon At Start", "Creature");
		colonAtStart.setCardText(": no cost.");
		final CardEntity notACost = card(4L, "Not A Cost", "Creature");
		notACost.setCardText("Flying: this does nothing.");

		given(baseFormat(),
				List.of(entry(20L, 1L, 1, "main"), entry(21L, 2L, 1, "main"), entry(22L, 3L, 1, "main"),
						entry(23L, 4L, 1, "main")),
				List.of(zirda, vanilla, noText, colonAtStart, notACost), legalitiesFor(4));

		final DeckValidationResult result = this.deckValidationService.evaluate(DECK_ID);

		assertThat(result.getIssues()).hasSize(4);
		assertThat(result.getIssues()).allMatch(issue -> issue.getCode() == DeckIssueCode.COMPANION_RULE_VIOLATION);
		assertThat(result.getIssues()).extracting(DeckIssue::getCardId).containsExactly(1L, 2L, 3L, 4L);
	}

	@Test
	@DisplayName("Should accept a Yorion deck with the extra twenty cards")
	void shouldAcceptYorionDeckWithEnoughCards() {
		this.deck.setCompanionCardId(99L);
		final CardEntity yorion = card(99L, "Yorion, Sky Nomad", "Creature");
		final CardEntity mountain = card(1L, "Mountain", "Land");
		mountain.setIsBasicLand(true);

		given(format(FORMAT_ID, 60, null, null, 4, false, true), List.of(entry(10L, 1L, 80, "main")),
				List.of(yorion, mountain), List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should skip companion rules when the companion has no name")
	void shouldSkipUnnamedCompanion() {
		this.deck.setCompanionCardId(99L);
		final CardEntity companion = card(99L, null, "Creature");
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");

		given(baseFormat(), List.of(entry(10L, 1L, 1, "main")), List.of(companion, bolt),
				List.of(legality(1L, FORMAT_ID, "legal")));

		assertThat(this.deckValidationService.evaluate(DECK_ID).getIssues()).isEmpty();
	}

	@Test
	@DisplayName("Should fall back to the format limit when the card is not restricted")
	void shouldFallBackWhenLegalityIsNotRestricted() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(bolt));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID))
				.thenReturn(Optional.of(legality(1L, FORMAT_ID, "legal")));
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.of(baseFormat()));

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(4);
	}

	@Test
	@DisplayName("Should throw when the card of a copy-limit lookup does not exist")
	void shouldThrowWhenCardMissing() {
		when(this.cardRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Card not found with id: 1");
	}

	@Test
	@DisplayName("Should return unlimited for a zero copies_limit")
	void shouldReturnUnlimitedForZeroCopiesLimit() {
		final CardEntity petitioners = card(1L, "Persistent Petitioners", "Creature");
		petitioners.setCopiesLimit(0);
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(petitioners));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID)).thenReturn(Optional.empty());

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(9999);
	}

	@Test
	@DisplayName("Should restrict a card with restricted legality to one copy")
	void shouldReturnOneForRestrictedCard() {
		final CardEntity recall = card(1L, "Ancestral Recall", "Instant");
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(recall));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID))
				.thenReturn(Optional.of(legality(1L, FORMAT_ID, "restricted")));

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(1);
	}

	@Test
	@DisplayName("Should restrict a singleton format to one copy")
	void shouldReturnOneForSingletonFormat() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(bolt));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(FORMAT_ID))
				.thenReturn(Optional.of(format(FORMAT_ID, 0, null, null, 4, true, true)));

		assertThat(this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID)).isEqualTo(1);
	}

	@Test
	@DisplayName("Should throw when the format of a copy-limit lookup does not exist")
	void shouldThrowWhenFormatMissingForCardLimit() {
		final CardEntity bolt = card(1L, "Lightning Bolt", "Instant");
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(bolt));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, FORMAT_ID)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.deckValidationService.getMaxAllowedQuantity(1L, FORMAT_ID))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Format not found with id: 1");
	}

	@Test
	@DisplayName("Should return max value when the format sets no section limits")
	void shouldReturnMaxValueForUnlimitedSections() {
		when(this.formatRepository.findById(FORMAT_ID))
				.thenReturn(Optional.of(format(FORMAT_ID, 0, null, null, 4, false, true)));

		assertThat(this.deckValidationService.getMaxDeckSize(FORMAT_ID, "main")).isEqualTo(Integer.MAX_VALUE);
		assertThat(this.deckValidationService.getMaxDeckSize(FORMAT_ID, "sideboard")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	@DisplayName("Should throw when asking the size of a missing format")
	void shouldThrowWhenFormatMissingForSize() {
		when(this.formatRepository.findById(FORMAT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.deckValidationService.getMaxDeckSize(FORMAT_ID, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Format not found with id: 1");
	}

	@Test
	@DisplayName("Should accept a removal within the available copies")
	void shouldAcceptValidRemoval() {
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(DECK_ID, 1L, "main"))
				.thenReturn(Optional.of(entry(10L, 1L, 2, "main")));

		this.deckValidationService.validateCardRemoval(DECK_ID, 1L, 2, "main");
	}

	@Test
	@DisplayName("Should reject a removal without a quantity")
	void shouldRejectNullRemoval() {
		assertThatThrownBy(() -> this.deckValidationService.validateCardRemoval(DECK_ID, 1L, null, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("greater than 0");
	}

	// ==================== FIXTURES ====================

	private void given(FormatEntity format, List<CardInDeckEntity> entries, List<CardEntity> cards,
			List<CardLegalityEntity> legalities) {
		when(this.deckRepository.findById(DECK_ID)).thenReturn(Optional.of(this.deck));
		when(this.formatRepository.findById(format.getId())).thenReturn(Optional.of(format));
		when(this.cardInDeckRepository.findByDeckId(DECK_ID)).thenReturn(entries);
		if (!entries.isEmpty()) {
			when(this.cardRepository.findAllById(any())).thenReturn(cards);
		}
		if (format.isTracksLegality() && !cards.isEmpty()) {
			when(this.cardLegalityRepository.findByFormatIdAndCardIdIn(eq(format.getId()), any()))
					.thenReturn(legalities);
		}
		if (this.deck.getCompanionCardId() != null) {
			final CardEntity companion = cards.stream()
					.filter(card -> card.getId().equals(this.deck.getCompanionCardId()))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Companion card missing from fixture"));
			when(this.cardRepository.findById(this.deck.getCompanionCardId())).thenReturn(Optional.of(companion));
		}
	}

	private static DeckIssue singleIssue(DeckValidationResult result) {
		assertThat(result.getIssues()).hasSize(1);
		return result.getIssues().get(0);
	}

	private static FormatEntity baseFormat() {
		return format(FORMAT_ID, 0, null, 15, 4, false, true);
	}

	private static FormatEntity format(long id, int minDeckSize, Integer maxDeckSize, Integer maxSideboardSize,
			int maxCopies, boolean singleton, boolean tracksLegality) {
		return FormatEntity.builder()
				.id(id)
				.name("Test Format")
				.minDeckSize(minDeckSize)
				.maxDeckSize(maxDeckSize)
				.maxSideboardSize(maxSideboardSize)
				.maxCopies(maxCopies)
				.singleton(singleton)
				.requiresCommander(false)
				.tracksLegality(tracksLegality)
				.build();
	}

	private static CardEntity card(long id, String name, String type) {
		final CardEntity card = new CardEntity();
		card.setId(id);
		card.setName(name);
		card.setCardType(type);
		card.setCmc(1);
		card.setIsBasicLand(false);
		return card;
	}

	private static CardInDeckEntity entry(long id, long cardId, int quantity, String section) {
		return CardInDeckEntity.builder()
				.id(id)
				.deckId(DECK_ID)
				.cardId(cardId)
				.quantity(quantity)
				.section(section)
				.build();
	}

	private static CardLegalityEntity legality(long cardId, long formatId, String status) {
		final CardLegalityEntity entity = new CardLegalityEntity();
		entity.setCardId(cardId);
		entity.setFormatId(formatId);
		entity.setLegalityStatus(status);
		return entity;
	}

	private static List<CardLegalityEntity> legalitiesFor(int cardCount) {
		final List<CardLegalityEntity> legalities = new ArrayList<>();
		for (long cardId = 1; cardId <= cardCount; cardId++) {
			legalities.add(legality(cardId, FORMAT_ID, "legal"));
		}
		return legalities;
	}
}
