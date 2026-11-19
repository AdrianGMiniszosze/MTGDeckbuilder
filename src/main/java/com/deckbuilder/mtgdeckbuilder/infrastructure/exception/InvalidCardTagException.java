package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Exception thrown when a card tag payload is invalid, such as a missing weight
 * or an out-of-range confidence.
 */
public class InvalidCardTagException extends DomainException {

	@Serial
	private static final long serialVersionUID = 1L;

	public InvalidCardTagException(String message) {
		super(message);
	}
}
