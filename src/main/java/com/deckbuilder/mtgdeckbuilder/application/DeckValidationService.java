package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;

/**
 * Service responsible for validating deck composition rules.
 * This service implements the business logic that was previously handled by database triggers.
 */
public interface DeckValidationService {

    /**
     * Validates whether a card can be added to a deck in the specified quantity and section.
     *
     * @param deckId the deck ID
     * @param cardId the card ID
     * @param quantity the quantity to add
     * @param section the section (main, sideboard, maybeboard)
     * @param isUpdate whether this is an update operation (affects total card count calculations)
     * @throws InvalidDeckCompositionException if the addition violates deck rules
     */
    void validateCardAddition(Long deckId, Long cardId, Integer quantity, String section, boolean isUpdate);

    /**
     * Validates whether a card removal is valid.
     *
     * @param deckId the deck ID
     * @param cardId the card ID
     * @param quantity the quantity to remove
     * @param section the section (main, sideboard, maybeboard)
     * @throws InvalidDeckCompositionException if the removal is invalid
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
