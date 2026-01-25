package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardInDeckRepository extends JpaRepository<CardInDeckEntity, Long> {
	List<CardInDeckEntity> findByDeckId(Long deckId);

	Optional<CardInDeckEntity> findByDeckIdAndCardIdAndSection(Long deckId, Long cardId, String section);

	void deleteByDeckIdAndCardIdAndSection(Long deckId, Long cardId, String section);

	boolean existsByDeckIdAndCardIdAndSection(Long deckId, Long cardId, String section);

	/**
	 * Calculate total cards in a deck section, excluding a specific card
	 */
	@Query("SELECT COALESCE(SUM(cid.quantity), 0) " +
		   "FROM CardInDeckEntity cid " +
		   "WHERE cid.deckId = :deckId AND cid.section = :section AND cid.cardId != :excludeCardId")
	Integer sumQuantityByDeckIdAndSectionExcludingCard(@Param("deckId") Long deckId,
													   @Param("section") String section,
													   @Param("excludeCardId") Long excludeCardId);

	/**
	 * Calculate total cards in a deck section
	 */
	@Query("SELECT COALESCE(SUM(cid.quantity), 0) " +
		   "FROM CardInDeckEntity cid " +
		   "WHERE cid.deckId = :deckId AND cid.section = :section")
	Integer sumQuantityByDeckIdAndSection(@Param("deckId") Long deckId, @Param("section") String section);
}
