package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.DeckValidationServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.*;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
		this.testDeck = new DeckEntity();
		this.testDeck.setId(1L);
		this.testDeck.setFormatId(1L);

		// Regular card (Lightning Bolt)
		this.regularCard = new CardEntity();
		this.regularCard.setId(1L);
		this.regularCard.setName("Lightning Bolt");
		this.regularCard.setCardType("Instant");
		this.regularCard.setCardSupertype(null);
		this.regularCard.setUnlimitedCopies(false);

		// Basic land (Mountain)
		this.basicLand = new CardEntity();
		this.basicLand.setId(2L);
		this.basicLand.setName("Mountain");
		this.basicLand.setCardType("Land");
		this.basicLand.setCardSupertype("Basic");
		this.basicLand.setUnlimitedCopies(false);

		// Banned card
		this.bannedCard = new CardEntity();
		this.bannedCard.setId(3L);
		this.bannedCard.setName("Black Lotus");
		this.bannedCard.setCardType("Artifact");
		this.bannedCard.setCardSupertype(null);
		this.bannedCard.setUnlimitedCopies(false);

		// Restricted card (local)
		final CardEntity restrictedCard = new CardEntity();
		restrictedCard.setId(4L);
		restrictedCard.setName("Ancestral Recall");
		restrictedCard.setCardType("Instant");
		restrictedCard.setCardSupertype(null);
		restrictedCard.setUnlimitedCopies(false);

		// Formats
		this.standardFormat = FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).maxSideboardSize(15)
				.build();

		// Card legalities
		this.bannedLegality = new CardLegalityEntity();
		this.bannedLegality.setCardId(3L);
		this.bannedLegality.setFormatId(1L);
		this.bannedLegality.setLegalityStatus("banned");

		final CardLegalityEntity restrictedLegality = new CardLegalityEntity();
		restrictedLegality.setCardId(4L);
		restrictedLegality.setFormatId(1L);
		restrictedLegality.setLegalityStatus("restricted");
	}

	@Test
	@DisplayName("Should inject service correctly")
	void shouldInjectServiceCorrectly() {
		assertThat(this.deckValidationService).isNotNull();
	}

	@Test
	@DisplayName("Should allow valid card addition")
	void shouldAllowValidCardAddition() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeck));
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.regularCard));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, 1L)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.standardFormat));
		when(this.cardInDeckRepository.sumQuantityByDeckIdAndSection(1L, "main")).thenReturn(0);

		// When & Then - Should not throw exception
		this.deckValidationService.validateCardAddition(1L, 1L, 4, "main", false);

		// Verify interactions - After optimization, in validateCardAddition only:
		// 1. cardRepository.findById called once in validateCardAddition
		// 2. cardLegalityRepository.findByCardIdAndFormatId called once (optimized!)
		// 3. formatRepository.findById called once in private getMaxAllowedQuantity
		verify(this.deckRepository).findById(1L);
		verify(this.cardRepository, times(1)).findById(1L);
		verify(this.cardLegalityRepository, times(1)).findByCardIdAndFormatId(1L, 1L);
	}

	@Test
	@DisplayName("Should skip validation for maybeboard")
	void shouldSkipValidationForMaybeboard() {
		// When & Then - Should not throw exception or call any repositories
		this.deckValidationService.validateCardAddition(1L, 1L, 100, "maybeboard", false);

		// Verify no validation calls were made
		verifyNoInteractions(this.deckRepository, this.cardRepository, this.cardLegalityRepository,
				this.formatRepository);
	}

	@Test
	@DisplayName("Should throw exception for banned card")
	void shouldThrowExceptionForBannedCard() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeck));
		when(this.cardRepository.findById(3L)).thenReturn(Optional.of(this.bannedCard));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(3L, 1L)).thenReturn(Optional.of(this.bannedLegality));
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.standardFormat));

		// When & Then
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(1L, 3L, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("banned");

		// Verify interactions - for banned cards, it fails early so
		// cardRepository.findById is only called once
		verify(this.deckRepository).findById(1L);
		verify(this.cardRepository).findById(3L);
		verify(this.cardLegalityRepository).findByCardIdAndFormatId(3L, 1L);
	}

	@Test
	@DisplayName("Should allow unlimited basic lands")
	void shouldAllowUnlimitedBasicLands() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeck));
		when(this.cardRepository.findById(2L)).thenReturn(Optional.of(this.basicLand));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(2L, 1L)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.standardFormat));
		when(this.cardInDeckRepository.sumQuantityByDeckIdAndSection(1L, "main")).thenReturn(40); // Already 40 cards in
																									// deck

		// When & Then - Should allow any quantity for basic lands (20 would exceed
		// normal 4-card limit)
		// This should NOT throw an exception even though we're adding 20 copies of a
		// card
		this.deckValidationService.validateCardAddition(1L, 2L, 20, "main", false);

		// Also verify that getMaxAllowedQuantity returns unlimited for basic lands
		final int maxAllowed = this.deckValidationService.getMaxAllowedQuantity(2L, 1L);
		assertThat(maxAllowed).isEqualTo(9999); // UNLIMITED_QUANTITY constant used by service

		// Verify interactions - This test makes TWO separate API calls:
		// 1. validateCardAddition() - optimized internally
		// 2. getMaxAllowedQuantity() - separate public API call
		// So cardRepository.findById is called twice total (once per API call)
		verify(this.deckRepository).findById(1L);
		verify(this.cardRepository, times(2)).findById(2L);
		verify(this.cardLegalityRepository, atLeast(1)).findByCardIdAndFormatId(2L, 1L);
	}

	@Test
	@DisplayName("Should return main deck size for getMaxDeckSize")
	void shouldReturnMainDeckSizeForGetMaxDeckSize() {
		// Given
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.standardFormat));

		// When
		final int maxDeckSize = this.deckValidationService.getMaxDeckSize(1L, "main");

		// Then
		assertThat(maxDeckSize).isEqualTo(60);
	}

	@Test
	@DisplayName("Should return 4 for regular cards max quantity")
	void shouldReturn4ForRegularCardsMaxQuantity() {
		// Given
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.regularCard));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, 1L)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.standardFormat));

		// When
		final int maxQuantity = this.deckValidationService.getMaxAllowedQuantity(1L, 1L);

		// Then
		assertThat(maxQuantity).isEqualTo(4);
	}

	@Test
	@DisplayName("Should throw exception for non-basic card quantity > 4")
	void shouldThrowExceptionForNonBasicCardQuantityGreaterThan4() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeck));
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.regularCard));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(1L, 1L)).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.standardFormat));

		// When & Then - Should throw exception for trying to add 5 copies of a
		// non-basic card
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(1L, 1L, 5, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("quantity limit");
	}

	@Test
	@DisplayName("Companion Lurrus: should reject permanent MV > 2 and allow MV <= 2")
	void companionLurrusValidation() {
		final Long deckId = 1L;
		final Long companionId = 1000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity lurrus = new CardEntity();
		lurrus.setId(companionId);
		lurrus.setName("Lurrus of the Dream-Den");

		final CardEntity permTooHigh = new CardEntity();
		permTooHigh.setId(10L);
		permTooHigh.setName("Permanent 3 MV");
		permTooHigh.setCardType("Artifact");
		permTooHigh.setCmc(3);

		final CardEntity permOk = new CardEntity();
		permOk.setId(11L);
		permOk.setName("Permanent 2 MV");
		permOk.setCardType("Enchantment");
		permOk.setCmc(2);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(lurrus));
		when(this.cardRepository.findById(permTooHigh.getId())).thenReturn(Optional.of(permTooHigh));
		when(this.cardRepository.findById(permOk.getId())).thenReturn(Optional.of(permOk));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));

		final Long badId = permTooHigh.getId();
		final Long okId = permOk.getId();
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, badId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Lurrus");

		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, okId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Obosh: should reject even MV nonland and allow odd MV")
	void companionOboshValidation() {
		final Long deckId = 2L;
		final Long companionId = 2000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity obosh = new CardEntity();
		obosh.setId(companionId);
		obosh.setName("Obosh, the Preypiercer");

		final CardEntity evenSpell = new CardEntity();
		evenSpell.setId(20L);
		evenSpell.setName("Even MV Spell");
		evenSpell.setCardType("Instant");
		evenSpell.setCmc(2);

		final CardEntity oddSpell = new CardEntity();
		oddSpell.setId(21L);
		oddSpell.setName("Odd MV Spell");
		oddSpell.setCardType("Sorcery");
		oddSpell.setCmc(3);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(obosh));
		when(this.cardRepository.findById(evenSpell.getId())).thenReturn(Optional.of(evenSpell));
		when(this.cardRepository.findById(oddSpell.getId())).thenReturn(Optional.of(oddSpell));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));

		final Long evenId = evenSpell.getId();
		final Long oddId = oddSpell.getId();
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, evenId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Obosh");

		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, oddId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Gyruda: should reject odd MV nonland and allow even MV")
	void companionGyrudaValidation() {
		final Long deckId = 3L;
		final Long companionId = 3000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity gyruda = new CardEntity();
		gyruda.setId(companionId);
		gyruda.setName("Gyruda, Doom of Depths");

		final CardEntity oddSpell = new CardEntity();
		oddSpell.setId(30L);
		oddSpell.setName("Odd MV Spell");
		oddSpell.setCardType("Sorcery");
		oddSpell.setCmc(3);

		final CardEntity evenSpell = new CardEntity();
		evenSpell.setId(31L);
		evenSpell.setName("Even MV Spell");
		evenSpell.setCardType("Instant");
		evenSpell.setCmc(2);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(gyruda));
		when(this.cardRepository.findById(oddSpell.getId())).thenReturn(Optional.of(oddSpell));
		when(this.cardRepository.findById(evenSpell.getId())).thenReturn(Optional.of(evenSpell));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));

		final Long oddId = oddSpell.getId();
		final Long evenId = evenSpell.getId();
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, oddId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Gyruda");

		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, evenId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Keruga: should reject MV <= 2 nonland and allow MV >= 3")
	void companionKerugaValidation() {
		final Long deckId = 4L;
		final Long companionId = 4000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity keruga = new CardEntity();
		keruga.setId(companionId);
		keruga.setName("Keruga, the Macrosage");

		final CardEntity mv2 = new CardEntity();
		mv2.setId(40L);
		mv2.setName("MV 2 nonland");
		mv2.setCardType("Enchantment");
		mv2.setCmc(2);

		final CardEntity mv3 = new CardEntity();
		mv3.setId(41L);
		mv3.setName("MV 3 nonland");
		mv3.setCardType("Artifact");
		mv3.setCmc(3);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(keruga));
		when(this.cardRepository.findById(mv2.getId())).thenReturn(Optional.of(mv2));
		when(this.cardRepository.findById(mv3.getId())).thenReturn(Optional.of(mv3));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));

		final Long badId = mv2.getId();
		final Long okId = mv3.getId();
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, badId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Keruga");

		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, okId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Kaheera: should reject creature subtype not in allowed list and allow allowed subtype")
	void companionKaheeraValidation() {
		final Long deckId = 5L;
		final Long companionId = 5000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity kaheera = new CardEntity();
		kaheera.setId(companionId);
		kaheera.setName("Kaheera, the Orphanguard");

		final CardEntity badCreature = new CardEntity();
		badCreature.setId(50L);
		badCreature.setName("Human Rogue");
		badCreature.setCardType("Creature");
		badCreature.setSubtypes(java.util.List.of("Human", "Rogue"));

		final CardEntity goodCreature = new CardEntity();
		goodCreature.setId(51L);
		goodCreature.setName("Elemental");
		goodCreature.setCardType("Creature");
		goodCreature.setSubtypes(java.util.List.of("Elemental"));

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(kaheera));
		when(this.cardRepository.findById(badCreature.getId())).thenReturn(Optional.of(badCreature));
		when(this.cardRepository.findById(goodCreature.getId())).thenReturn(Optional.of(goodCreature));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));

		final Long badId = badCreature.getId();
		final Long okId = goodCreature.getId();
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, badId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Kaheera");

		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, okId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Jegantha: should reject repeated mana symbols and allow non-repeating costs")
	void companionJeganthaValidation() {
		final Long deckId = 6L;
		final Long companionId = 6000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity jegantha = new CardEntity();
		jegantha.setId(companionId);
		jegantha.setName("Jegantha, the Wellspring");

		// Failing: RR (raw) and WWU (brace form) -> repeats of R and W
		final CardEntity rrSpell = new CardEntity();
		rrSpell.setId(60L);
		rrSpell.setName("RR Spell");
		rrSpell.setCardType("Instant");
		rrSpell.setManaCost("RR");
		rrSpell.setCmc(2);

		final CardEntity wwuSpell = new CardEntity();
		wwuSpell.setId(61L);
		wwuSpell.setName("WWU Spell");
		wwuSpell.setCardType("Sorcery");
		wwuSpell.setManaCost("{W}{W}{U}");
		wwuSpell.setCmc(3);

		// Passing: R/G (hybrid) and 2R (numeral + single R)
		final CardEntity hybridRG = new CardEntity();
		hybridRG.setId(62L);
		hybridRG.setName("Hybrid RG");
		hybridRG.setCardType("Instant");
		hybridRG.setManaCost("{R/G}");
		hybridRG.setCmc(1);

		final CardEntity twoR = new CardEntity();
		twoR.setId(63L);
		twoR.setName("Two and R");
		twoR.setCardType("Sorcery");
		twoR.setManaCost("{2}{R}");
		twoR.setCmc(3);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(jegantha));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));

		when(this.cardRepository.findById(rrSpell.getId())).thenReturn(Optional.of(rrSpell));
		when(this.cardRepository.findById(wwuSpell.getId())).thenReturn(Optional.of(wwuSpell));
		when(this.cardRepository.findById(hybridRG.getId())).thenReturn(Optional.of(hybridRG));
		when(this.cardRepository.findById(twoR.getId())).thenReturn(Optional.of(twoR));

		final Long rrId = rrSpell.getId();
		final Long wwuId = wwuSpell.getId();
		final Long rgId = hybridRG.getId();
		final Long twoRId = twoR.getId();

		// Fail cases
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, rrId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Jegantha");

		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, wwuId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Jegantha");

		// Pass cases
		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, rgId, 1, "main", false));
		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, twoRId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Umori: should reject mixed nonland types and allow single type")
	void companionUmoriValidation() {
		final Long deckId = 7L;
		final Long companionId = 7000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity umori = new CardEntity();
		umori.setId(companionId);
		umori.setName("Umori, the Collector");

		// Existing main deck has Instants only
		final CardInDeckEntity existingInstantEntry = CardInDeckEntity.builder().id(701L).deckId(deckId).cardId(7010L)
				.quantity(4).section("main").build();
		final CardEntity existingInstant = new CardEntity();
		existingInstant.setId(7010L);
		existingInstant.setName("Existing Instant");
		existingInstant.setCardType("Instant");
		existingInstant.setCmc(1);

		// Candidate different type (Artifact) -> should fail
		final CardEntity artifactCard = new CardEntity();
		artifactCard.setId(7020L);
		artifactCard.setName("New Artifact");
		artifactCard.setCardType("Artifact");
		artifactCard.setCmc(2);

		// Candidate same type (Instant) -> should pass
		final CardEntity instantCard = new CardEntity();
		instantCard.setId(7030L);
		instantCard.setName("New Instant");
		instantCard.setCardType("Instant");
		instantCard.setCmc(2);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(umori));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));
		// Deck composition
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(java.util.List.of(existingInstantEntry));
		when(this.cardRepository.findById(existingInstantEntry.getCardId())).thenReturn(Optional.of(existingInstant));
		// Candidate lookups
		when(this.cardRepository.findById(artifactCard.getId())).thenReturn(Optional.of(artifactCard));
		when(this.cardRepository.findById(instantCard.getId())).thenReturn(Optional.of(instantCard));

		final Long artifactId = artifactCard.getId();
		final Long instantId = instantCard.getId();

		// Fail: adding Artifact introduces mixed nonland types
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, artifactId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Umori");

		// Pass: adding Instant keeps single nonland type
		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, instantId, 1, "main", false));
	}

	@Test
	@DisplayName("Companion Yorion: should enforce main deck size >= min+20")
	void companionYorionValidation() {
		final Long deckId = 8L;
		final Long companionId = 8000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity yorion = new CardEntity();
		yorion.setId(companionId);
		yorion.setName("Yorion, Sky Nomad");

		final CardEntity candidate = new CardEntity();
		candidate.setId(8010L);
		candidate.setName("Filler Card");
		candidate.setCardType("Instant");
		candidate.setCmc(1);

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(yorion));
		when(this.cardRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L)).thenReturn(
				Optional.of(FormatEntity.builder().id(1L).name("Standard").minDeckSize(60).maxDeckSize(80).build()));

		// Case 1: current main deck size is 79, adding 1 -> 80 (min+20), should pass
		when(this.cardInDeckRepository.sumQuantityByDeckIdAndSection(deckId, "main")).thenReturn(79);
		assertThatNoException().isThrownBy(
				() -> this.deckValidationService.validateCardAddition(deckId, candidate.getId(), 1, "main", false));

		// Case 2: current main deck size is 78, adding 1 -> 79 (< min+20), should fail
		when(this.cardInDeckRepository.sumQuantityByDeckIdAndSection(deckId, "main")).thenReturn(78);
		assertThatThrownBy(
				() -> this.deckValidationService.validateCardAddition(deckId, candidate.getId(), 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Yorion");
	}

	@Test
	@DisplayName("Companion Zirda: should reject permanents without activated abilities and allow those with them")
	void companionZirdaValidation() {
		final Long deckId = 9L;
		final Long companionId = 9000L;
		final DeckEntity deck = new DeckEntity();
		deck.setId(deckId);
		deck.setFormatId(1L);
		deck.setCompanionCardId(companionId);

		final CardEntity zirda = new CardEntity();
		zirda.setId(companionId);
		zirda.setName("Zirda, the Dawnwaker");

		// Existing permanent without activated ability (static text)
		final CardInDeckEntity existingPermanentEntry = CardInDeckEntity.builder().id(901L).deckId(deckId).cardId(9010L)
				.quantity(2).section("main").build();
		final CardEntity existingPermanentNoAbility = new CardEntity();
		existingPermanentNoAbility.setId(9010L);
		existingPermanentNoAbility.setName("Static Enchantment");
		existingPermanentNoAbility.setCardType("Enchantment");
		existingPermanentNoAbility.setCardText("Creatures you control get +1/+1.");

		// Candidate permanent without activated ability -> should fail
		final CardEntity candidateNoAbility = new CardEntity();
		candidateNoAbility.setId(9020L);
		candidateNoAbility.setName("Vanilla Artifact");
		candidateNoAbility.setCardType("Artifact");
		candidateNoAbility.setCardText("This artifact does nothing.");

		// Candidate permanent with activated ability -> should pass
		final CardEntity candidateWithAbility = new CardEntity();
		candidateWithAbility.setId(9030L);
		candidateWithAbility.setName("Tapping Equipment");
		candidateWithAbility.setCardType("Artifact");
		candidateWithAbility.setCardText("{T}: Equipped creature gets +1/+0 until end of turn.");

		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
		when(this.cardRepository.findById(companionId)).thenReturn(Optional.of(zirda));
		when(this.cardLegalityRepository.findByCardIdAndFormatId(anyLong(), anyLong())).thenReturn(Optional.empty());
		when(this.formatRepository.findById(1L))
				.thenReturn(Optional.of(FormatEntity.builder().id(1L).name("Standard").maxDeckSize(60).build()));
		// Deck composition
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(java.util.List.of(existingPermanentEntry));
		when(this.cardRepository.findById(existingPermanentEntry.getCardId()))
				.thenReturn(Optional.of(existingPermanentNoAbility));
		// Candidate lookups
		when(this.cardRepository.findById(candidateNoAbility.getId())).thenReturn(Optional.of(candidateNoAbility));
		when(this.cardRepository.findById(candidateWithAbility.getId())).thenReturn(Optional.of(candidateWithAbility));

		final Long badId = candidateNoAbility.getId();
		final Long okId = candidateWithAbility.getId();

		// Fail: adding a permanent without activated ability violates Zirda
		assertThatThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, badId, 1, "main", false))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Zirda");

		// Re-mock deck composition for pass case: no conflicting permanents in main
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(java.util.List.of());
		// Pass: adding a permanent with activated ability is allowed
		assertThatNoException()
				.isThrownBy(() -> this.deckValidationService.validateCardAddition(deckId, okId, 1, "main", false));
	}
}
