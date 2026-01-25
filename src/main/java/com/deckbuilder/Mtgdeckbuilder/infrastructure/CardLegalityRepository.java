package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardLegalityRepository extends JpaRepository<CardLegalityEntity, CardLegalityId> {

    /**
     * Find the legality status of a card in a specific format
     */
    Optional<CardLegalityEntity> findByCardIdAndFormatId(Long cardId, Long formatId);

    /**
     * Check if a card is banned in a specific format
     */
    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END " +
           "FROM CardLegalityEntity cl " +
           "WHERE cl.cardId = :cardId AND cl.formatId = :formatId AND cl.legalityStatus = 'banned'")
    boolean isCardBanned(@Param("cardId") Long cardId, @Param("formatId") Long formatId);

    /**
     * Check if a card is restricted in a specific format
     */
    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END " +
           "FROM CardLegalityEntity cl " +
           "WHERE cl.cardId = :cardId AND cl.formatId = :formatId AND cl.legalityStatus = 'restricted'")
    boolean isCardRestricted(@Param("cardId") Long cardId, @Param("formatId") Long formatId);
}
