package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Card-level data needed to upsert one Scryfall printing into {@code cards}.
 *
 * <p>
 * Child rows (attributes, legalities, faces) travel in
 * {@link ImportCardAttributes}; prices and set data are card columns. Keeping
 * one immutable snapshot per printing lets the DAO write every row inside the
 * same unit of work.
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
		boolean variation,
		String setName,
		String setCode,
		LocalDate setReleasedAt,
		String setType,
		Map<String, BigDecimal> prices,
		ImportCardAttributes attributes) {
}
