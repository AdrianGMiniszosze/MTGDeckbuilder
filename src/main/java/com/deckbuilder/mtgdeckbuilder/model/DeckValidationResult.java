package com.deckbuilder.mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of evaluating a deck against the rules of its format.
 *
 * <p>
 * It carries no exception for rule violations: the caller decides what to do
 * with {@link #getErrors()}. Publishing rejects when there is at least one
 * error; a draft is simply saved with its diagnosis attached.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckValidationResult {
	private Long deckId;
	private DeckStatus status;
	private ValidationStatus validationStatus;
	private List<DeckIssue> issues;

	public List<DeckIssue> getErrors() {
		return this.issues == null ? List.of()
				: this.issues.stream().filter(issue -> issue.getSeverity() == IssueSeverity.ERROR).toList();
	}

	public List<DeckIssue> getWarnings() {
		return this.issues == null ? List.of()
				: this.issues.stream().filter(issue -> issue.getSeverity() == IssueSeverity.WARNING).toList();
	}

	public boolean hasErrors() {
		return !this.getErrors().isEmpty();
	}
}
