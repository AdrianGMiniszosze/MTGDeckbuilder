package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Exception thrown when deck composition violates format rules
 */
public class InvalidDeckCompositionException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

	public InvalidDeckCompositionException(String message) {
		super(message);
	}

	public InvalidDeckCompositionException(String message, Throwable cause) {
		super(message, cause);
	}
}
