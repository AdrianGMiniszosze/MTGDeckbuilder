package com.deckbuilder.Mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "card_deck")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardInDeckEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(name = "deck_id", nullable = false)
    private Long deckId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, length = 20)
    private String section;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", insertable = false, updatable = false)
    private DeckEntity deck;
}