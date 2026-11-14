package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRulingId implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private UUID oracleId;
	private LocalDate publishedAt;
	private String comment;
	private String source;

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		CardRulingId that = (CardRulingId) other;
		return Objects.equals(oracleId, that.oracleId)
				&& Objects.equals(publishedAt, that.publishedAt)
				&& Objects.equals(comment, that.comment)
				&& Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(oracleId, publishedAt, comment, source);
	}
}
