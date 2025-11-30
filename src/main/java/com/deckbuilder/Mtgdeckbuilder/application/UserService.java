package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
	List<User> findAll(int pageSize, int pageNumber);
	Optional<User> findById(Long id);
	User create(User user);
	User update(Long id, User user);
	void deleteById(Long id);
	List<Deck> getUserDecks(Long userId, int pageSize, int pageNumber);
}
