package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when a card violates format legality rules
 */
public class FormatViolationException extends DomainException {

	public FormatViolationException(String cardName, String formatName, String reason) {
		super(String.format("Card '%s' cannot be used in format '%s': %s", cardName, formatName, reason));
	}
}
