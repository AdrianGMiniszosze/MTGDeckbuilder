package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Card;

import java.util.List;
import java.util.Optional;

public interface CardService {
	// Basic CRUD operations
	List<Card> getAllCards(int pageSize, int pageNumber);
	Optional<Card> getCardById(Long id);
	Card createCard(Card card);
	Optional<Card> updateCard(Long id, Card card);
	void deleteCard(Long id);

	// Search and query operations
	List<Card> searchCards(String query, int pageSize, int pageNumber);
	List<Card> getCardsByFormat(Long formatId);

	// Variant-specific operations
	List<Card> findByCollectorNumber(String collectorNumber);
	List<Card> findByNameAndSet(String name, Long setId);

	// Similarity search operations (for future ML features)
	List<Card> findSimilarCards(Double[] vector, int maxResults);
	List<Card> findSimilarToCard(Long cardId, int maxResults);

	// Alias methods for test compatibility
	default Optional<Card> findById(Long id) {
		return getCardById(id);
	}

	default Card create(Card card) {
		return createCard(card);
	}
}