package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CompanionRules;
import com.deckbuilder.mtgdeckbuilder.application.DeckValidationService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.*;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckValidationServiceImpl implements DeckValidationService {

	private final DeckRepository deckRepository;
	private final CardRepository cardRepository;
	private final FormatRepository formatRepository;
	private final CardLegalityRepository cardLegalityRepository;
	private final CardInDeckRepository cardInDeckRepository;

	private static final int SIDEBOARD_MAX_SIZE = 15;
	private static final int DEFAULT_MAX_CARD_QUANTITY = 4;
	private static final int UNLIMITED_QUANTITY = 9999;

	@Override
	public void validateCardAddition(Long deckId, Long cardId, Integer quantity, String section, boolean isUpdate) {
		log.debug("Validating card addition: deckId={}, cardId={}, quantity={}, section={}, isUpdate={}", deckId,
				cardId, quantity, section, isUpdate);

		// Skip validation for maybeboard cards
		if ("maybeboard".equals(section)) {
			return;
		}

		this.validateRequestedQuantity(quantity);

		final DeckEntity deck = this.getDeckOrThrow(deckId);
		if (deck.getFormatId() == null) {
			throw new InvalidDeckCompositionException("Deck format is not specified");
		}

		final CardEntity card = this.getCardOrThrow(cardId);

		// Apply companion constraints only when adding to main deck
		if ("main".equals(section) && deck.getCompanionCardId() != null) {
			this.applyCompanionConstraints(deck, card, quantity);
		}

		// Legality and quantity checks
		final Optional<CardLegalityEntity> cardLegalityOpt = this.cardLegalityRepository.findByCardIdAndFormatId(cardId,
				deck.getFormatId());
		this.validateCardLegality(cardLegalityOpt.orElse(null), deck.getFormatId());

		final int maxAllowedQuantity = this.getMaxAllowedQuantity(card, cardLegalityOpt.orElse(null),
				deck.getFormatId());
		if (quantity > maxAllowedQuantity) {
			throw new InvalidDeckCompositionException(
					String.format("Card quantity limit exceeded. Max allowed is %d, attempted to add %d",
							maxAllowedQuantity, quantity));
		}

		// Deck size limit check
		this.validateDeckSizeLimit(deckId, quantity, section, deck.getFormatId(), isUpdate ? cardId : null);
	}

	@Override
	public void validateCardRemoval(Long deckId, Long cardId, Integer quantity, String section) {
		log.debug("Validating card removal: deckId={}, cardId={}, quantity={}, section={}", deckId, cardId, quantity,
				section);

		if (quantity == null || quantity <= 0) {
			throw new InvalidDeckCompositionException("Quantity must be greater than 0");
		}

		// Check if card exists in this section
		final Optional<CardInDeckEntity> existingCard = this.cardInDeckRepository
				.findByDeckIdAndCardIdAndSection(deckId, cardId, section);

		if (existingCard.isEmpty()) {
			throw new InvalidDeckCompositionException("Card not found in deck section");
		}

		final int currentQuantity = existingCard.get().getQuantity();
		if (quantity > currentQuantity) {
			throw new InvalidDeckCompositionException(
					String.format("Cannot remove %d cards, only %d available", quantity, currentQuantity));
		}
	}

	@Override
	public int getMaxAllowedQuantity(Long cardId, Long formatId) {
		// Get card details and legality
		final CardEntity card = this.cardRepository.findById(cardId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Card not found with id: " + cardId));

		final Optional<CardLegalityEntity> cardLegality = this.cardLegalityRepository.findByCardIdAndFormatId(cardId,
				formatId);

		return this.getMaxAllowedQuantity(card, cardLegality.orElse(null), formatId);
	}

	/**
	 * Internal method that calculates max allowed quantity using pre-fetched
	 * entities to avoid redundant database calls when card and legality are already
	 * known.
	 */
	private int getMaxAllowedQuantity(CardEntity card, CardLegalityEntity cardLegality, Long formatId) {
		// Check if card has unlimited copies flag
		if (Boolean.TRUE.equals(card.getUnlimitedCopies())) {
			return UNLIMITED_QUANTITY;
		}

		// Check if it's a basic land
		if ("Land".equals(card.getCardType()) && "Basic".equals(card.getCardSupertype())) {
			return UNLIMITED_QUANTITY;
		}

		// Check if card is restricted in this format
		if (cardLegality != null && "restricted".equals(cardLegality.getLegalityStatus())) {
			return 1;
		}

		// Check format-specific rules
		final FormatEntity format = this.formatRepository.findById(formatId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Format not found with id: " + formatId));

		if ("Commander".equals(format.getName())) {
			return 1;
		}

		// Default maximum
		return DEFAULT_MAX_CARD_QUANTITY;
	}

	@Override
	public int getMaxDeckSize(Long formatId, String section) {
		if ("sideboard".equals(section)) {
			return SIDEBOARD_MAX_SIZE;
		}

		// For main deck, use format's deck size limit
		final FormatEntity format = this.formatRepository.findById(formatId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Format not found with id: " + formatId));

		return format.getMaxDeckSize();
	}

	private void validateCardLegality(CardLegalityEntity cardLegality, Long formatId) {
		if (cardLegality != null) {
			final String legalityStatus = cardLegality.getLegalityStatus();
			if ("banned".equals(legalityStatus)) {
				final FormatEntity format = this.formatRepository.findById(formatId)
						.orElseThrow(() -> new InvalidDeckCompositionException("Format not found"));
				throw new InvalidDeckCompositionException(
						String.format("Card is banned in the %s format", format.getName()));
			}
		}
		// If no legality record exists, assume the card is legal
	}

	private void validateDeckSizeLimit(Long deckId, Integer quantity, String section, Long formatId,
			Long excludeCardId) {
		final int maxDeckSize = this.getMaxDeckSize(formatId, section);

		final Integer currentTotalCards;
		if (excludeCardId != null) {
			// This is an update operation, exclude the existing quantity of this card
			currentTotalCards = this.cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(deckId, section,
					excludeCardId);
		} else {
			// This is a new addition
			currentTotalCards = this.cardInDeckRepository.sumQuantityByDeckIdAndSection(deckId, section);
		}

		final int totalAfterAddition = (currentTotalCards != null ? currentTotalCards : 0) + quantity;

		if (totalAfterAddition > maxDeckSize) {
			throw new InvalidDeckCompositionException(String.format(
					"Deck section \"%s\" size limit exceeded. Max allowed is %d, total after addition would be %d",
					section, maxDeckSize, totalAfterAddition));
		}
	}

	// Helper: determine if a card is a permanent (Artifact, Creature, Enchantment,
	// Planeswalker, Land)
	private boolean isPermanent(CardEntity c) {
		final String t = c.getCardType();
		if (t == null)
			return false;
		return t.contains("Artifact") || t.contains("Creature") || t.contains("Enchantment")
				|| t.contains("Planeswalker") || t.contains("Land");
	}

	// Helper: heuristic to detect activated ability in card text: looks for a colon
	// with cost-like left side
	private boolean hasActivatedAbility(CardEntity c) {
		final String text = c.getCardText();
		if (text == null)
			return false;
		final String lower = text.toLowerCase();
		// Heuristics:
		// - Look for patterns like "{T}:", "{1}:", "{r}:" etc (mana/tap symbols before
		// colon)
		// - Or common cost words before a colon: "tap:", "sacrifice:", "discard:",
		// "exile:", "return:", "pay:", "remove:", "untap:"
		final String normalized = lower.replace("\n", " ");
		if (normalized.contains(":")) {
			final String[] clauses = normalized.split("\\.");
			for (final String clause : clauses) {
				final String s = clause.trim();
				final int idx = s.indexOf(":");
				if (idx > 0) {
					final String left = s.substring(0, idx).trim();
					if (left.contains("{") || left.contains("tap") || left.contains("{t}") || left.matches(".*[0-9].*")
							|| left.startsWith("sacrifice") || left.startsWith("discard") || left.startsWith("exile")
							|| left.startsWith("return") || left.startsWith("pay") || left.startsWith("remove")
							|| left.startsWith("untap")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void validateRequestedQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new InvalidDeckCompositionException("Quantity must be greater than 0");
		}
	}

	private DeckEntity getDeckOrThrow(Long deckId) {
		return this.deckRepository.findById(deckId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Deck not found with id: " + deckId));
	}

	private CardEntity getCardOrThrow(Long cardId) {
		return this.cardRepository.findById(cardId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Card not found with id: " + cardId));
	}

	private void applyCompanionConstraints(DeckEntity deck, CardEntity candidateCard, int candidateQuantity) {
		final CardEntity companion = this.cardRepository.findById(deck.getCompanionCardId()).orElse(null);
		if (companion == null) {
			return;
		}
		final String companionName = companion.getName() != null ? companion.getName().toLowerCase() : "";

		// Per-card rule check via CompanionRules
		final String violation = CompanionRules.validateByCompanionName(companion.getName(), candidateCard);
		if (violation != null) {
			throw new InvalidDeckCompositionException(violation);
		}

		// Deck-level constraints by companion
		if (companionName.contains("umori")) {
			this.enforceUmoriDeckTypeRule(deck.getId(), candidateCard);
		}
		if (companionName.contains("yorion")) {
			this.enforceYorionDeckSizeRule(deck, candidateQuantity);
		}
		if (companionName.contains("zirda")) {
			this.enforceZirdaActivatedAbilityRule(deck.getId(), candidateCard);
		}
	}

	private void enforceUmoriDeckTypeRule(Long deckId, CardEntity candidateCard) {
		final Set<String> nonlandTypes = new HashSet<>();
		final List<CardInDeckEntity> allEntries = this.cardInDeckRepository.findByDeckId(deckId);
		for (final CardInDeckEntity cid : allEntries) {
			if (!"main".equals(cid.getSection()))
				continue;
			final CardEntity existing = this.cardRepository.findById(cid.getCardId()).orElse(null);
			if (existing == null)
				continue;
			final String type = existing.getCardType();
			if (type != null && !type.contains("Land")) {
				nonlandTypes.add(type);
			}
		}
		final String candidateType = candidateCard.getCardType();
		if (candidateType != null && !candidateType.contains("Land")) {
			nonlandTypes.add(candidateType);
		}
		if (nonlandTypes.size() > 1) {
			throw new InvalidDeckCompositionException(
					"Companion Umori: all nonland cards must share the same card type");
		}
	}

	private void enforceYorionDeckSizeRule(DeckEntity deck, int candidateQuantity) {
		final FormatEntity format = this.formatRepository.findById(deck.getFormatId()).orElseThrow(
				() -> new InvalidDeckCompositionException("Format not found with id: " + deck.getFormatId()));
		final int requiredMin = format.getMinDeckSize() + 20;
		final Integer currentMain = this.cardInDeckRepository.sumQuantityByDeckIdAndSection(deck.getId(), "main");
		final int afterAddition = (currentMain != null ? currentMain : 0) + candidateQuantity;
		if (afterAddition < requiredMin) {
			throw new InvalidDeckCompositionException(
					String.format("Companion Yorion: main deck must be at least %d cards", requiredMin));
		}
	}

	private void enforceZirdaActivatedAbilityRule(Long deckId, CardEntity candidateCard) {
		final List<CardInDeckEntity> allEntries = this.cardInDeckRepository.findByDeckId(deckId);
		for (final CardInDeckEntity cid : allEntries) {
			if (!"main".equals(cid.getSection()))
				continue;
			final CardEntity existing = this.cardRepository.findById(cid.getCardId()).orElse(null);
			if (existing == null)
				continue;
			if (this.isPermanent(existing) && !this.hasActivatedAbility(existing)) {
				throw new InvalidDeckCompositionException(
						"Companion Zirda: each permanent card in your main deck must have an activated ability");
			}
		}
		if (this.isPermanent(candidateCard) && !this.hasActivatedAbility(candidateCard)) {
			throw new InvalidDeckCompositionException(
					"Companion Zirda: each permanent card in your main deck must have an activated ability");
		}
	}
}
