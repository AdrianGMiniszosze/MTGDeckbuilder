package com.deckbuilder.mtgdeckbuilder.application;

public interface AuthEmailSender {

	void sendVerification(String email, String rawToken);

	void sendPasswordReset(String email, String rawToken);
}
