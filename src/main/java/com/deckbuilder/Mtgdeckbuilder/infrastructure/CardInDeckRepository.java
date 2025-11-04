package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardInDeckRepository extends JpaRepository<CardInDeckEntity, Long> {
    List<CardInDeckEntity> findByDeckId(Long deckId);
    
    Optional<CardInDeckEntity> findByDeckIdAndCardIdAndSection(Long deckId, Long cardId, String section);
    
    void deleteByDeckIdAndCardIdAndSection(Long deckId, Long cardId, String section);
    
    boolean existsByDeckIdAndCardIdAndSection(Long deckId, Long cardId, String section);
}
