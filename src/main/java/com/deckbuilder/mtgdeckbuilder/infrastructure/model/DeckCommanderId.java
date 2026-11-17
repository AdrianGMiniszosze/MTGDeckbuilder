package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link DeckCommanderEntity}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeckCommanderId implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Long deckId;
	private Long cardId;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DeckCommanderId that = (DeckCommanderId) o;
		return Objects.equals(this.deckId, that.deckId) && Objects.equals(this.cardId, that.cardId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.deckId, this.cardId);
	}
}
