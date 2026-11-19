package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

public record AuthRateLimitDecision(boolean allowed, long retryAfterSeconds, long requestCount) {
}
