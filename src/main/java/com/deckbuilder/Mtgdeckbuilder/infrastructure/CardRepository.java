package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long> {

	@Query("SELECT c FROM CardEntity c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
	Page<CardEntity> searchByName(@Param("query") String query, Pageable pageable);

	// Query cards that are legal in a specific format using the card_legality table
	@Query(value = "SELECT c.* FROM cards c " + "INNER JOIN card_legality cl ON c.id = cl.card_id "
			+ "WHERE cl.format_id = :formatId AND cl.legality_status != 'banned'", nativeQuery = true)
	List<CardEntity> findByFormatId(@Param("formatId") Long formatId);

	// Query cards that are legal in a specific format with pagination
	@Query(value = "SELECT c.* FROM cards c " + "INNER JOIN card_legality cl ON c.id = cl.card_id "
			+ "WHERE cl.format_id = :formatId AND cl.legality_status != 'banned'", countQuery = "SELECT COUNT(c.id) FROM cards c "
					+ "INNER JOIN card_legality cl ON c.id = cl.card_id "
					+ "WHERE cl.format_id = :formatId AND cl.legality_status != 'banned'", nativeQuery = true)
	Page<CardEntity> findByFormatId(@Param("formatId") Long formatId, Pageable pageable);

	// Query methods for card variants
	List<CardEntity> findByCollectorNumber(String collectorNumber);

	List<CardEntity> findByNameAndCardSet(String name, Long cardSet);

	// TODO: Implement with proper pgvector query when vector search is needed
	// For now, this method should not be used as it requires pgvector extension
	// List<CardEntity> findSimilarByEmbedding(Double[] embedding, int maxResults);
}