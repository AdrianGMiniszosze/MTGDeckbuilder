package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Custom repository interface for complex card search operations using EntityManager
 */
public interface CardRepositoryCustom {

    /**
     * Advanced search with detailed criteria using EntityManager for dynamic query building
     */
    Page<CardEntity> searchCardsWithDetailedCriteria(CardSearchCriteria criteria, Pageable pageable);

    /**
     * Random card selection with optional filters using EntityManager
     */
    List<CardEntity> findRandomCards(int count, String type, String rarity, Long formatId);
}
