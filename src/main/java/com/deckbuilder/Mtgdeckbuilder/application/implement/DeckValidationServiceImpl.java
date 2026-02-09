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

		if (quantity == null || quantity <= 0) {
			throw new InvalidDeckCompositionException("Quantity must be greater than 0");
		}

		// Get deck and its format
		final DeckEntity deck = this.deckRepository.findById(deckId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Deck not found with id: " + deckId));

		if (deck.getFormatId() == null) {
			throw new InvalidDeckCompositionException("Deck format is not specified");
		}

		// Get card
		final CardEntity card = this.cardRepository.findById(cardId)
				.orElseThrow(() -> new InvalidDeckCompositionException("Card not found with id: " + cardId));

		// Companion constraints apply only to main section additions
		if ("main".equals(section) && deck.getCompanionCardId() != null) {
			// Find companion card to determine rules by name
			final CardEntity companion = this.cardRepository.findById(deck.getCompanionCardId()).orElse(null);
			if (companion != null) {
				final String violation = CompanionRules.validateByCompanionName(companion.getName(), card);
				if (violation != null) {
					throw new InvalidDeckCompositionException(violation);
				}
				// Umori deck-level check
				if (companion.getName() != null && companion.getName().toLowerCase().contains("umori")) {
					// Gather existing nonland card types in main deck by fetching all and filtering
					// section
					final Set<String> nonlandTypes = new HashSet<>();
					final List<CardInDeckEntity> allEntries = this.cardInDeckRepository.findByDeckId(deckId);
					for (final CardInDeckEntity cid : allEntries) {
						if (!"main".equals(cid.getSection()))
							continue; // consider starting deck only
						final CardEntity existing = this.cardRepository.findById(cid.getCardId()).orElse(null);
						if (existing == null)
							continue;
						if (existing.getCardType() != null && !existing.getCardType().contains("Land")) {
							nonlandTypes.add(existing.getCardType());
						}
					}
					// Include the new card type if nonland
					if (card.getCardType() != null && !card.getCardType().contains("Land")) {
						nonlandTypes.add(card.getCardType());
					}
					// Rule: there must be at most 1 distinct nonland type
					if (nonlandTypes.size() > 1) {
						throw new InvalidDeckCompositionException(
								"Companion Umori: all nonland cards must share the same card type");
					}
				}
				// Yorion, Sky Nomad: starting deck contains at least 20 cards more than the
				// minimum deck size
				if (companion.getName() != null && companion.getName().toLowerCase().contains("yorion")) {
					final FormatEntity format = this.formatRepository.findById(deck.getFormatId())
							.orElseThrow(() -> new InvalidDeckCompositionException(
									"Format not found with id: " + deck.getFormatId()));
					// Base required minimum is format's min main deck size, require +20 with Yorion
					final int requiredMin = format.getMinDeckSize() + 20;
					final Integer currentMain = this.cardInDeckRepository.sumQuantityByDeckIdAndSection(deckId, "main");
					final int afterAddition = (currentMain != null ? currentMain : 0) + quantity;
					if (afterAddition < requiredMin) {
						throw new InvalidDeckCompositionException(
								String.format("Companion Yorion: main deck must be at least %d cards", requiredMin));
					}
				}
				// Zirda, the Dawnwaker: each permanent card in starting deck has an activated
				// ability
				if (companion.getName() != null && companion.getName().toLowerCase().contains("zirda")) {
					final List<CardInDeckEntity> allEntries = this.cardInDeckRepository.findByDeckId(deckId);
					for (final CardInDeckEntity cid : allEntries) {
						if (!"main".equals(cid.getSection()))
							continue; // starting deck only
						final CardEntity existing = this.cardRepository.findById(cid.getCardId()).orElse(null);
						if (existing == null)
							continue;
						if (this.isPermanent(existing) && !this.hasActivatedAbility(existing)) {
							throw new InvalidDeckCompositionException(
									"Companion Zirda: each permanent card in your main deck must have an activated ability");
						}
					}
					// Also validate the candidate card if it's a permanent
					if (this.isPermanent(card) && !this.hasActivatedAbility(card)) {
						throw new InvalidDeckCompositionException(
								"Companion Zirda: each permanent card in your main deck must have an activated ability");
					}
				}
			}
		}

		// Get card legality once and reuse it
		final Optional<CardLegalityEntity> cardLegality = this.cardLegalityRepository.findByCardIdAndFormatId(cardId,
				deck.getFormatId());

		// Check card legality
		this.validateCardLegality(cardLegality, deck.getFormatId());

		// Check individual card quantity limits
		final int maxAllowedQuantity = this.getMaxAllowedQuantity(card, cardLegality, deck.getFormatId());
		if (quantity > maxAllowedQuantity) {
			throw new InvalidDeckCompositionException(
					String.format("Card quantity limit exceeded. Max allowed is %d, attempted to add %d",
							maxAllowedQuantity, quantity));
		}

		// Check deck size limits
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

		return this.getMaxAllowedQuantity(card, cardLegality, formatId);
	}

	/**
	 * Internal method that calculates max allowed quantity using pre-fetched
	 * entities to avoid redundant database calls when card and legality are already
	 * known.
	 */
	private int getMaxAllowedQuantity(CardEntity card, Optional<CardLegalityEntity> cardLegality, Long formatId) {
		// Check if card has unlimited copies flag
		if (Boolean.TRUE.equals(card.getUnlimitedCopies())) {
			return UNLIMITED_QUANTITY;
		}

		// Check if it's a basic land
		if ("Land".equals(card.getCardType()) && "Basic".equals(card.getCardSupertype())) {
			return UNLIMITED_QUANTITY;
		}

		// Check if card is restricted in this format
		if (cardLegality.isPresent() && "restricted".equals(cardLegality.get().getLegalityStatus())) {
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

	private void validateCardLegality(Optional<CardLegalityEntity> cardLegality, Long formatId) {
		if (cardLegality.isPresent()) {
			final String legalityStatus = cardLegality.get().getLegalityStatus();
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
		// Quick reject: triggered ability starters
		if (lower.startsWith("when ") || lower.startsWith("whenever ") || lower.startsWith("at the ")) {
			// Could still contain other activated abilities below; don't early return here
		}
		// Heuristics:
		// - Look for patterns like "{T}:", "{1}:", "{r}:" etc (mana/tap symbols before
		// colon)
		// - Or common cost words before a colon: "tap:", "sacrifice:", "discard:",
		// "exile:", "return:", "pay:", "remove:", "untap:"
		final String normalized = lower.replace("\n", " ");
		if (normalized.contains(":")) {
			// Split clauses by period to examine segments
			final String[] clauses = normalized.split("\\.");
			for (final String clause : clauses) {
				final String s = clause.trim();
				final int idx = s.indexOf(":");
				if (idx > 0) {
					final String left = s.substring(0, idx).trim();
					// Indicators of an activated cost
					if (left.contains("{") // mana symbols like {1}{r}{t}
							|| left.contains("tap") || left.contains("{t}") || left.matches(".*[0-9].*")
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
}
