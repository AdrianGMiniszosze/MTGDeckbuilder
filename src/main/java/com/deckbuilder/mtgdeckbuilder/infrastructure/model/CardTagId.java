package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for CardTagEntity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardTagId implements Serializable {
	private Long cardId;
	private Long tagId;
}
