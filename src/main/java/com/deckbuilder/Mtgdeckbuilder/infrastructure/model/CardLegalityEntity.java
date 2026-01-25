package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_legality")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CardLegalityId.class)
public class CardLegalityEntity {

    @Id
    @Column(name = "card_id")
    private Long cardId;

    @Id
    @Column(name = "format_id")
    private Long formatId;

    @Column(name = "legality_status", nullable = false)
    private String legalityStatus;

    @ManyToOne
    @JoinColumn(name = "card_id", insertable = false, updatable = false)
    private CardEntity card;

    @ManyToOne
    @JoinColumn(name = "format_id", insertable = false, updatable = false)
    private FormatEntity format;
}
