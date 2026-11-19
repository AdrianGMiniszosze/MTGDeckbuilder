package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

public class RateLimitStoreUnavailableException extends DomainException {

	public RateLimitStoreUnavailableException() {
		super("Authentication rate-limit service is temporarily unavailable");
	}
}
