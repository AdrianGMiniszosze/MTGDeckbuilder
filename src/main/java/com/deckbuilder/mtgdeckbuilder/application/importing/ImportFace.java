package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * One face of a multi-face Scryfall printing.
 *
 * <p>
 * Stored in {@code card_faces} with {@code face_index} taken from list order.
 * The raw object is kept in {@link #faceData()} so fields that are not
 * normalized yet are not lost.
 */
public record ImportFace(
		String name,
		String manaCost,
		String typeLine,
		String oracleText,
		List<String> colors,
		List<String> colorIndicator,
		String power,
		String toughness,
		String loyalty,
		String defense,
		JsonNode imageUris,
		JsonNode faceData) {
}
