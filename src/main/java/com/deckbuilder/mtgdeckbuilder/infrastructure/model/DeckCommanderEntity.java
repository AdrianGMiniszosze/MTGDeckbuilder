package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Marks a card as the commander of a deck. The card itself lives in
 * {@code card_deck}; this row only carries the role.
 */
@Entity
@Table(name = "deck_commander")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(DeckCommanderId.class)
public class DeckCommanderEntity {

	@Id
	@Column(name = "deck_id", nullable = false)
	private Long deckId;

	@Id
	@Column(name = "card_id", nullable = false)
	private Long cardId;
}
