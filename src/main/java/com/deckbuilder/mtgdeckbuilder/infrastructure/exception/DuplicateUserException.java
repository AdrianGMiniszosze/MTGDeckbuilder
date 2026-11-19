package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * A username or email is already registered.
 */
public class DuplicateUserException extends DomainException {

	public DuplicateUserException(String field, String value) {
		super("A user with that " + field + " already exists: " + value);
	}
}
