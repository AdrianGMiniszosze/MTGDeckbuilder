package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Deck;

import java.util.List;
import java.util.Optional;

public interface DeckService {
	List<Deck> getAll(int pageSize, int pageNumber);
	Optional<Deck> findById(Long id);
	List<Deck> findByUserId(Long userId, int pageSize, int pageNumber);
	List<Deck> findByFormat(Long formatId);
	Deck create(Deck deck);
	Deck update(Long id, Deck deck);
	boolean deleteById(Long id);

	// Card management
	Deck addCard(Long deckId, Long cardId, int quantity, String section);
	Deck removeCard(Long deckId, Long cardId, int quantity, String section);
}