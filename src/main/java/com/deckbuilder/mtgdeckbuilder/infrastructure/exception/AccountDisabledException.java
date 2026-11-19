package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * The account exists but cannot be used to log in.
 */
public class AccountDisabledException extends DomainException {

	public AccountDisabledException() {
		super("The account is disabled");
	}
}
