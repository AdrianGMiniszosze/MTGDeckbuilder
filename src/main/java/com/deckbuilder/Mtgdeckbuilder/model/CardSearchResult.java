package com.deckbuilder.mtgdeckbuilder.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result object containing cards and pagination information from search operations
 */
@Data
@Builder
public class CardSearchResult {
    private List<Card> cards;
    private Integer totalCount;

    public static CardSearchResult of(List<Card> cards, Integer totalCount) {
        return CardSearchResult.builder()
            .cards(cards)
            .totalCount(totalCount)
            .build();
    }
}
