package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;

/**
 * Evaluates deck composition against the rules of its format.
 *
 * <p>
 * Nothing here rejects a save: saving is always allowed and the result is a
 * diagnosis. Publishing is what turns {@code errors} into a rejection
 * (ADR-007). Format policy lives only in Java; the database enforces data shape.
 */
public interface DeckValidationService {

	/**
	 * Evaluates the current contents of a deck.
	 *
	 * @param deckId the deck to evaluate
	 * @return the diagnosis; never throws for rule violations
	 * @throws com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException
	 *             if the deck does not exist
	 * @throws com.deckbuilder.mtgdeckbuilder.infrastructure.exception.FormatNotFoundException
	 *             if the deck's format does not exist
	 */
	DeckValidationResult evaluate(Long deckId);

	/**
	 * Validates whether a card removal is possible. This is data integrity (the
	 * row and quantity must exist), not a format rule.
	 *
	 * @param deckId the deck ID
	 * @param cardId the card ID
	 * @param quantity the quantity to remove
	 * @param section the section (main, sideboard, maybeboard)
	 * @throws com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException
	 *             if the removal is invalid
	 */
	void validateCardRemoval(Long deckId, Long cardId, Integer quantity, String section);

	/**
	 * Gets the maximum allowed quantity for a specific card in a format.
	 *
	 * @param cardId the card ID
	 * @param formatId the format ID
	 * @return maximum allowed quantity
	 */
	int getMaxAllowedQuantity(Long cardId, Long formatId);

	/**
	 * Gets the maximum deck size for a specific section and format.
	 *
	 * @param formatId the format ID
	 * @param section the section (main, sideboard, maybeboard)
	 * @return maximum deck size for that section
	 */
	int getMaxDeckSize(Long formatId, String section);
}
