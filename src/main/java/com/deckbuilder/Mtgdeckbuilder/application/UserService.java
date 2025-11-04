package com.deckbuilder.Mtgdeckbuilder.application;

import com.deckbuilder.Mtgdeckbuilder.model.User;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
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
