package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Card;

import java.util.List;
import java.util.Optional;

public interface CardService {
	List<Card> getAllCards(int pageSize, int pageNumber);
	Optional<Card> getCardById(Long id);
	List<Card> searchCards(String query, int pageSize, int pageNumber);
	List<Card> getCardsByFormat(Long formatId);
	Card createCard(Card card);
	Optional<Card> updateCard(Long id, Card card);
	void deleteCard(Long id);
	List<Card> findSimilarCards(Double[] vector, int maxResults);
	List<Card> findSimilarToCard(Long cardId, int maxResults);
}