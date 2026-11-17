package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.util.List;
import java.util.Map;

/**
 * Child rows of one printing: attribute tables, format legalities and faces.
 *
 * <p>
 * The importer replaces these rows for every card it sees (D-027), so the
 * lists and maps here always describe the full current state.
 */
public record ImportCardAttributes(
		List<String> colors,
		List<String> colorIdentity,
		List<String> types,
		List<String> supertypes,
		List<String> subtypes,
		List<String> keywords,
		Map<String, String> legalities,
		List<ImportFace> faces) {
}
