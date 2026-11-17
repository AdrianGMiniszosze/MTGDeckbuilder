package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.util.UUID;

/**
 * Card-level data needed to upsert one Scryfall printing into {@code cards}.
 *
 * <p>
 * Child rows (colors, types, legalities, faces, keywords) and prices are added
 * by later importer phases, so this record stays a snapshot of the card row.
 */
public record ImportCard(
		UUID scryfallId,
		UUID oracleId,
		String name,
		String manaCost,
		int cmc,
		String colorIdentity,
		String typeLine,
		String cardType,
		String cardSupertype,
		String rarity,
		String cardText,
		String flavorText,
		String power,
		String toughness,
		Integer copiesLimit,
		boolean basicLand,
		String imageUrl,
		boolean foil,
		boolean gameChanger,
		String language,
		String collectorNumber,
		boolean promo,
		boolean variation) {
}
