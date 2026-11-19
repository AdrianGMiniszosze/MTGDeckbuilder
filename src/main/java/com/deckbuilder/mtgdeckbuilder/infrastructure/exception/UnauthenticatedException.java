package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * No authenticated caller was found where one is required.
 */
public class UnauthenticatedException extends DomainException {

	public UnauthenticatedException() {
		super("Authentication is required");
	}
}
