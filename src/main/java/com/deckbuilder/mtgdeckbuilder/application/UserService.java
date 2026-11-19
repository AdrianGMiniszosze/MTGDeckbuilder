package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
	List<User> findAll(int pageSize, int pageNumber);
	Optional<User> findById(Long id);
	Optional<User> findByUsername(String username);
	Optional<User> findByUsernameOrEmail(String identifier);
	User create(User user);
	User update(Long id, User user);
	User recordLogin(Long id);
	void deleteById(Long id);
	List<Deck> getUserDecks(Long userId, int pageSize, int pageNumber);
}
