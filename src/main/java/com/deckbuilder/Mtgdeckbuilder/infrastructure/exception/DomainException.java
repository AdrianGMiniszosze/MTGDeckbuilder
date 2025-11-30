package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Base exception for all domain-specific exceptions
 */
public abstract class DomainException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	protected DomainException(String message) {
		super(message);
	}

	protected DomainException(String message, Throwable cause) {
		super(message, cause);
	}
}
