package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for CardLegalityEntity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLegalityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long cardId;
    private Long formatId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardLegalityId that = (CardLegalityId) o;
        return Objects.equals(cardId, that.cardId) && Objects.equals(formatId, that.formatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, formatId);
    }
}
