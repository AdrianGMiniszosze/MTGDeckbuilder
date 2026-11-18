package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One ruling from the Scryfall rulings bulk.
 *
 * <p>
 * Scryfall has no ruling id: the natural identity is the full tuple
 * {@code (oracleId, publishedAt, comment, source)}. The importer uses that
 * tuple to mirror the current bulk into {@code card_rulings}.
 */
public record ImportRuling(UUID oracleId, LocalDate publishedAt, String comment, String source) {
}
