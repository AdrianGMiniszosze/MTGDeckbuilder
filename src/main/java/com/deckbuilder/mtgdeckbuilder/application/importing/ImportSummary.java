package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.time.Duration;

/**
 * Counters and flags produced by one import run.
 *
 * <p>
 * Row failures are counted, not thrown: the caller turns them into exit code 1.
 * {@code partial} means {@code --import.limit} was used; in that case neither
 * card obsolescence nor the ruling mirror runs. {@code sweepSkipped} and
 * {@code rulingMirrorSkipped} explain why a destructive step was skipped.
 */
public record ImportSummary(
		int cardsInserted,
		int cardsUpdated,
		int cardsSkipped,
		int cardsFailed,
		int cardsObsolete,
		int rulingsStaged,
		int rulingsInserted,
		int rulingsDeleted,
		int rulingsSkipped,
		int rulingsFailed,
		boolean partial,
		boolean sweepSkipped,
		boolean rulingMirrorSkipped,
		Duration duration) {

	public boolean hasFailures() {
		return cardsFailed > 0 || rulingsFailed > 0;
	}

	@Override
	public String toString() {
		return ("cards inserted=%d updated=%d skipped=%d failed=%d obsolete=%d; "
				+ "rulings staged=%d inserted=%d deleted=%d skipped=%d failed=%d; "
				+ "partial=%s sweepSkipped=%s rulingMirrorSkipped=%s; duration=%dms")
			.formatted(cardsInserted, cardsUpdated, cardsSkipped, cardsFailed, cardsObsolete,
					rulingsStaged, rulingsInserted, rulingsDeleted, rulingsSkipped, rulingsFailed,
					partial, sweepSkipped, rulingMirrorSkipped, duration.toMillis());
	}
}
