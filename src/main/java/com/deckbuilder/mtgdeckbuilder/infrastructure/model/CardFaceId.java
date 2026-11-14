package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardFaceId implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private Long cardId;
	private Short faceIndex;

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		CardFaceId that = (CardFaceId) other;
		return Objects.equals(cardId, that.cardId) && Objects.equals(faceIndex, that.faceIndex);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cardId, faceIndex);
	}
}
