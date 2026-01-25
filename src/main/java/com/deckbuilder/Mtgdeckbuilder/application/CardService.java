package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchResult;

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

	// Multi-face card operations
	Card getCardByIdRequired(Long id); // Returns Card or throws exception if not found

	// Enhanced search operations that return combined cards for faces
	List<Card> searchCardsByType(String cardType, int pageSize, int pageNumber);
	List<Card> searchCardsByName(String name, int pageSize, int pageNumber);
	List<Card> searchCardsAdvanced(String name, String cardType, String rarity, int pageSize, int pageNumber);

	// Advanced search with multiple criteria
	CardSearchResult searchCardsWithCriteria(CardSearchCriteria criteria, int pageSize, int pageNumber);

	// Random card operations
	List<Card> getRandomCards(int count, String type, String rarity, Long formatId);

	// Alias methods for test compatibility
	default Optional<Card> findById(Long id) {
		return getCardById(id);
	}

	default Card create(Card card) {
		return createCard(card);
	}
}