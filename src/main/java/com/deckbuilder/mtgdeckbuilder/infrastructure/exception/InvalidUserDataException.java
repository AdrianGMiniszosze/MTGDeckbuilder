package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when a user payload cannot be accepted, for example when the
 * password is missing on creation.
 */
public class InvalidUserDataException extends DomainException {

	public InvalidUserDataException(String message) {
		super(message);
	}
}
