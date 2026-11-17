package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckCommanderEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckCommanderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DeckCommanderRepository extends JpaRepository<DeckCommanderEntity, DeckCommanderId> {

	List<DeckCommanderEntity> findByDeckId(Long deckId);

	/**
	 * Loads the commanders of several decks in one query, so list endpoints do
	 * not issue one query per deck.
	 */
	List<DeckCommanderEntity> findByDeckIdIn(Collection<Long> deckIds);

	void deleteByDeckId(Long deckId);
}
