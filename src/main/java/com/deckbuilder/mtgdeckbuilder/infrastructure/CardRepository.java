package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	 * Find cards the current Scryfall bulk still contains, newest import first.
	 */
	Page<CardEntity> findByIsObsoleteFalse(Pageable pageable);

	/**
	 * Find cards by collector number
	 */
	List<CardEntity> findByCollectorNumber(String collectorNumber);

	/**
	 * Find cards by name and set
	 */
	List<CardEntity> findByNameAndCardSetId(String name, Long cardSetId);
}