package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

public class InvalidAccountActionTokenException extends DomainException {

	public InvalidAccountActionTokenException() {
		super("Invalid or expired account action token");
	}
}
