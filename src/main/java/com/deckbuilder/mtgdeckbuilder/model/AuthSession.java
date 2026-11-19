package com.deckbuilder.mtgdeckbuilder.model;

/**
 * Result of a successful login or refresh. The access token is a JWT; the
 * refresh token is an opaque, single-use value.
 */
public record AuthSession(String accessToken, String refreshToken, long expiresIn) {
}
