package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for CardTagEntity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardTagId implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Long cardId;
	private Long tagId;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || this.getClass() != o.getClass())
			return false;
		final CardTagId that = (CardTagId) o;
		return Objects.equals(this.cardId, that.cardId) && Objects.equals(this.tagId, that.tagId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.cardId, this.tagId);
	}
}

