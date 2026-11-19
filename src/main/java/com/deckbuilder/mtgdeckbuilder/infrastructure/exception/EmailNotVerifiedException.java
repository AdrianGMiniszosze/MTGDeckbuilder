package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

public class EmailNotVerifiedException extends DomainException {

	public EmailNotVerifiedException() {
		super("Verify your email address before logging in");
	}
}
