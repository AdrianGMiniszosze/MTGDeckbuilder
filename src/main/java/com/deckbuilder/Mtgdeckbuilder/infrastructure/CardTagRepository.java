package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardTagRepository extends JpaRepository<CardTagEntity, CardTagId> {
    
    /**
     * Find all card-tag relationships for a specific card
     */
    List<CardTagEntity> findByCardId(Long cardId);
    
    /**
     * Find all card-tag relationships for a specific tag
     */
    List<CardTagEntity> findByTagId(Long tagId);
    
    /**
     * Find cards by tag with minimum confidence threshold
     */
    @Query("SELECT ct FROM CardTagEntity ct WHERE ct.tagId = :tagId AND ct.confidence >= :minConfidence")
    List<CardTagEntity> findByTagIdWithMinConfidence(
        @Param("tagId") Long tagId, 
        @Param("minConfidence") Double minConfidence
    );
}
