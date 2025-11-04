package com.deckbuilder.Mtgdeckbuilder.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Deck {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime created;
    private LocalDateTime modified;
    private String deckType;
    private Boolean isPrivate;
    private String tournament;
    private String shareUrl;
    private Long parentDeckId;
    private Long formatId;
    private Long userId;
    private List<CardInDeck> cards;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardInDeck {
        private Long id;
        private Long cardId;
        private Long deckId;
        private int quantity;
        private String section;
    }
}