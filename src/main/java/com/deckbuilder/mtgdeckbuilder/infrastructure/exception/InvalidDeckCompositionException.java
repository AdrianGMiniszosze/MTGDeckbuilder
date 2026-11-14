package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when deck composition violates format rules
 */
public class InvalidDeckCompositionException extends DomainException {

	public InvalidDeckCompositionException(String message) {
		super(message);
	}
}
