package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to validate Companion constraints based on the selected
 * companion card. This keeps rules centralized and testable without DB
 * triggers.
 */
public final class CompanionRules {
	private CompanionRules() {
	}

	/**
	 * Validate a single card under specific companion rule. Examples below
	 * implement common companions.
	 */
	public static String validateByCompanionName(String companionName, CardEntity card) {
		if (companionName == null || card == null)
			return null;
		final String n = companionName.toLowerCase();

		// Ordered map: key substring -> validator function (returns violation or null)
		final Map<String, Function<CardEntity, String>> rules = new LinkedHashMap<>();
		rules.put("lurrus",
				c -> (isPermanent(c) && c.getCmc() != null && c.getCmc() > 2)
						? "Companion Lurrus: permanents must have mana value 2 or less"
						: null);
		rules.put("obosh",
				c -> (!isLand(c) && c.getCmc() != null && c.getCmc() % 2 == 0)
						? "Companion Obosh: nonland cards must have odd mana value"
						: null);
		rules.put("gyruda",
				c -> (!isLand(c) && c.getCmc() != null && c.getCmc() % 2 != 0)
						? "Companion Gyruda: nonland cards must have even mana value"
						: null);
		rules.put("keruga",
				c -> (!isLand(c) && c.getCmc() != null && c.getCmc() <= 2)
						? "Companion Keruga: nonland cards must have mana value 3 or greater"
						: null);
		rules.put("kaheera", c -> {
			if (isCreature(c)) {
				final boolean ok = hasAnySubtype(c, "Cat", "Elemental", "Nightmare", "Dinosaur", "Beast");
				return ok
						? null
						: "Companion Kaheera: creature cards must be Cat, Elemental, Nightmare, Dinosaur, or Beast";
			}
			return null;
		});
		// Jegantha, the Wellspring: no card has more than one of the same mana symbol
		// in its mana cost
		rules.put("jegantha", c -> {
			final String violation = violatesJeganthaManaCostRule(c.getManaCost());
			return violation;
		});

		for (final Map.Entry<String, Function<CardEntity, String>> e : rules.entrySet()) {
			if (n.contains(e.getKey())) {
				return e.getValue().apply(card);
			}
		}
		// Umori, Zirda, Yorion deck-level rules -> not per-card here
		return null;
	}

	private static boolean isLand(CardEntity c) {
		return c.getCardType() != null && c.getCardType().contains("Land");
	}
	private static boolean isPermanent(CardEntity c) {
		final String t = c.getCardType();
		if (t == null)
			return false;
		return t.contains("Artifact") || t.contains("Creature") || t.contains("Enchantment")
				|| t.contains("Planeswalker") || t.contains("Land");
	}
	private static boolean isCreature(CardEntity c) {
		return c.getCardType() != null && c.getCardType().contains("Creature");
	}
	private static boolean hasAnySubtype(CardEntity c, String... subtypes) {
		if (c.getSubtypes() == null)
			return false;
		for (final String s : subtypes) {
			for (final String cs : c.getSubtypes()) {
				if (cs.equalsIgnoreCase(s))
					return true;
			}
		}
		return false;
	}

	/**
	 * Returns a violation message if manaCost breaks Jegantha's restriction, else
	 * null. Handles common formats like "{1}{R}{R}", hybrid "{R/G}", phyrexian
	 * "{R/P}", colorless "{C}", snow "{S}". Ignores numerals and X.
	 */
	private static String violatesJeganthaManaCostRule(String manaCost) {
		if (manaCost == null || manaCost.isEmpty())
			return null;
		// Normalize: ensure braces-based splitting; also support raw strings like "RR"
		// by early detection
		final Map<String, Integer> counts = new LinkedHashMap<>();
		// Track symbols of interest
		final String[] symbols = {"W", "U", "B", "R", "G", "C", "S"};
		// Split by braces if present
		if (manaCost.contains("{")) {
			final String[] parts = manaCost.split("\\}");
			for (final String part : parts) {
				final int open = part.indexOf('{');
				if (open >= 0) {
					final String token = part.substring(open + 1).toUpperCase(); // e.g., "R", "R/G", "2", "X", "R/P"
					if (token.isEmpty())
						continue;
					if (token.equals("X") || token.matches("^[0-9]+$"))
						continue; // ignore X and numerals
					// For hybrid or phyrexian like "R/G" or "R/P", increment each letter color
					final String[] sub = token.split("/");
					for (final String s : sub) {
						final String sym = s.trim();
						for (final String target : symbols) {
							if (sym.equals(target)) {
								counts.put(target, counts.getOrDefault(target, 0) + 1);
							}
						}
					}
				}
			}
		} else {
			// Raw form like "RR" or "WWU"; iterate characters
			final String raw = manaCost.toUpperCase();
			for (int i = 0; i < raw.length(); i++) {
				final char ch = raw.charAt(i);
				if (ch == 'X')
					continue;
				if (Character.isDigit(ch))
					continue;
				final String t = String.valueOf(ch);
				for (final String target : symbols) {
					if (t.equals(target)) {
						counts.put(target, counts.getOrDefault(target, 0) + 1);
					}
				}
			}
		}
		// Check for duplicates
		for (final Map.Entry<String, Integer> e : counts.entrySet()) {
			if (e.getValue() != null && e.getValue() > 1) {
				return "Companion Jegantha: card mana cost contains multiple '" + e.getKey() + "' symbols";
			}
		}
		return null;
	}
}
