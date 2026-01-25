package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Basic JPA repository for CardEntity with simple query methods.
 * Complex searches are handled by CardRepositoryCustom interface.
 */
@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long>, CardRepositoryCustom {

	/**
	 * Find cards by collector number
	 */
	List<CardEntity> findByCollectorNumber(String collectorNumber);

	/**
	 * Find cards by name and card set
	 */
	List<CardEntity> findByNameAndCardSet(String name, Long cardSet);
}