package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

public class RateLimitExceededException extends DomainException {

	private final long retryAfterSeconds;

	public RateLimitExceededException(long retryAfterSeconds) {
		super("Too many authentication requests. Retry later.");
		this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
	}

	public long getRetryAfterSeconds() {
		return this.retryAfterSeconds;
	}
}
