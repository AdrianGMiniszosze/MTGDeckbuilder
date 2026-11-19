package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Login failed. The message is deliberately generic so callers cannot probe
 * whether the username exists.
 */
public class InvalidCredentialsException extends DomainException {

	public InvalidCredentialsException() {
		super("Invalid username or password");
	}
}
