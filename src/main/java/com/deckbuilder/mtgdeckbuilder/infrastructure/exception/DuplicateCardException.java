package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when attempting to add a duplicate card beyond format limits
 */
public class DuplicateCardException extends DomainException {

	public DuplicateCardException(String cardName, int currentQuantity, int maxAllowed) {
		super(String.format("Cannot add card '%s'. Current quantity: %d, Maximum allowed: %d", cardName,
				currentQuantity, maxAllowed));
	}
}
