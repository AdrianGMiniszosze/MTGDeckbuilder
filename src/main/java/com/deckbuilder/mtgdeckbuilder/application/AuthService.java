package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.AuthSession;

public interface AuthService {

	AuthSession login(String identifier, String rawPassword);

	AuthSession refresh(String refreshToken);

	void logout(String refreshToken);
}
