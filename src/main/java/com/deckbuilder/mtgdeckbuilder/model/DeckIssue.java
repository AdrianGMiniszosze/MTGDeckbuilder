package com.deckbuilder.mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One problem found by the deck validator.
 *
 * <p>
 * Issues are a diagnosis, not an exception: saving never fails because of them.
 * {@code cardId} and {@code deckContentId} let the client highlight the row;
 * they are null when the issue is about the deck as a whole.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckIssue {
	private IssueSeverity severity;
	private DeckIssueCode code;
	private String message;
	private Long cardId;
	private Long deckContentId;
	private String section;
}
