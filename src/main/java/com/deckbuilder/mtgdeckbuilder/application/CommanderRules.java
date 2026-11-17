package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Commander legality rules that can be answered from a card row alone.
 *
 * <p>
 * Commander availability is a printed property: a legendary creature, a card
 * that says "can be your commander", or a Background/Doctor that only works as
 * the second half of a pair. Partner variants are read from the keyword list
 * first and from the rules text as a fallback, because both are imported from
 * Scryfall and older rows may lack keywords.
 *
 * <p>
 * The rules live here instead of in the validator so each pairing can be
 * tested without a database.
 */
public final class CommanderRules {

	private static final Pattern PARTNER_WITH = Pattern
		.compile("partner with ([^\\n(]+)", Pattern.CASE_INSENSITIVE);

	private static final String PARTNER = "partner";
	private static final String FRIENDS_FOREVER = "friends forever";
	private static final String CHOOSE_A_BACKGROUND = "choose a background";
	private static final String DOCTORS_COMPANION = "doctor's companion";

	private CommanderRules() {
	}

	/**
	 * A card that is a legal commander by itself: a legendary creature or any
	 * card whose text says it can be the commander (for example legendary
	 * planeswalkers such as Teferi, Temporal Archmage).
	 */
	public static boolean isCommanderOnItsOwn(CardEntity card) {
		return isLegendaryCreature(card) || saysCanBeCommander(card);
	}

	/**
	 * Backgrounds only work as the second commander next to a "Choose a
	 * Background" commander; alone they are not legal.
	 */
	public static boolean isBackground(CardEntity card) {
		return hasSubtype(card, "background") && contains(card.getTypeLine(), "enchantment");
	}

	public static boolean isDoctor(CardEntity card) {
		return hasSubtype(card, "doctor") && contains(card.getTypeLine(), "creature");
	}

	/**
	 * Explains why a single declared commander is invalid, or returns null when
	 * it is a legal commander on its own.
	 */
	public static String validateAlone(CardEntity card) {
		if (isCommanderOnItsOwn(card)) {
			return null;
		}
		if (isBackground(card)) {
			return String.format(
					"%s is a Background; it can only be a commander next to a commander with \"Choose a Background\".",
					name(card));
		}
		return String.format("%s cannot be a commander: it is not a legendary creature and its text does not allow it.",
				name(card));
	}

	/**
	 * Explains why two declared commanders cannot be paired, or returns null for
	 * a supported partner combination.
	 */
	public static String validatePair(CardEntity first, CardEntity second) {
		if (isPartnerWithPair(first, second) || isGenericPartnerPair(first, second)
				|| isFriendsForeverPair(first, second) || isBackgroundPair(first, second)
				|| isDoctorsCompanionPair(first, second)) {
			return null;
		}
		return String.format("%s and %s cannot be commanders together.", name(first), name(second));
	}

	public static boolean isPartnerWithPair(CardEntity first, CardEntity second) {
		return partnerWithName(first).map(target -> sameName(target, second.getName())).orElse(false)
				&& partnerWithName(second).map(target -> sameName(target, first.getName())).orElse(false);
	}

	public static boolean isGenericPartnerPair(CardEntity first, CardEntity second) {
		return isGenericPartner(first) && isGenericPartner(second);
	}

	public static boolean isFriendsForeverPair(CardEntity first, CardEntity second) {
		return hasKeyword(first, FRIENDS_FOREVER) && hasKeyword(second, FRIENDS_FOREVER);
	}

	public static boolean isBackgroundPair(CardEntity first, CardEntity second) {
		return (hasKeyword(first, CHOOSE_A_BACKGROUND) && isBackground(second))
				|| (hasKeyword(second, CHOOSE_A_BACKGROUND) && isBackground(first));
	}

	public static boolean isDoctorsCompanionPair(CardEntity first, CardEntity second) {
		final boolean firstCompanion = hasKeyword(first, DOCTORS_COMPANION) && isDoctor(second);
		final boolean secondCompanion = hasKeyword(second, DOCTORS_COMPANION) && isDoctor(first);
		return firstCompanion || secondCompanion;
	}

	/**
	 * Parses {@code cards.color_identity} ("W,U") into its color letters.
	 */
	public static Set<String> colorIdentity(CardEntity card) {
		final String raw = card.getColorIdentity();
		if (raw == null || raw.isBlank()) {
			return Set.of();
		}
		final Set<String> colors = new TreeSet<>();
		for (final String part : raw.split(",")) {
			final String color = part.trim().toUpperCase(Locale.ROOT);
			if (!color.isEmpty()) {
				colors.add(color);
			}
		}
		return Collections.unmodifiableSet(colors);
	}

	/**
	 * Name this card partners with, read from the rules text. Empty when the
	 * card is not a "Partner with" card.
	 */
	public static Optional<String> partnerWithName(CardEntity card) {
		if (card.getCardText() == null) {
			return Optional.empty();
		}
		final Matcher matcher = PARTNER_WITH.matcher(card.getCardText().replace('\u2019', '\''));
		if (!matcher.find()) {
			return Optional.empty();
		}
		return Optional.of(matcher.group(1).replaceAll("[\\s.,;:]+$", "").trim());
	}

	private static boolean isGenericPartner(CardEntity card) {
		if (card.getKeywords() != null
				&& card.getKeywords().stream().filter(Objects::nonNull).anyMatch(PARTNER::equalsIgnoreCase)) {
			return true;
		}
		// Fallback for rows without keywords: "partner" must not be the
		// "Partner with <name>" variant, which needs the name to match.
		return partnerWithName(card).isEmpty() && contains(card.getCardText(), PARTNER);
	}

	private static boolean hasKeyword(CardEntity card, String keyword) {
		if (card.getKeywords() != null && card.getKeywords()
			.stream()
			.filter(Objects::nonNull)
			.anyMatch(value -> value.equalsIgnoreCase(keyword))) {
			return true;
		}
		return contains(card.getCardText(), keyword);
	}

	private static boolean isLegendaryCreature(CardEntity card) {
		return contains(card.getTypeLine(), "legendary") && contains(card.getTypeLine(), "creature");
	}

	private static boolean saysCanBeCommander(CardEntity card) {
		return contains(card.getCardText(), "can be your commander");
	}

	private static boolean hasSubtype(CardEntity card, String subtype) {
		return card.getSubtypes() != null
				&& card.getSubtypes().stream().filter(Objects::nonNull).anyMatch(value -> value.equalsIgnoreCase(subtype));
	}

	private static boolean sameName(String first, String second) {
		return first != null && second != null
				&& normalize(first).replaceAll("\\s+", " ").trim()
					.equalsIgnoreCase(normalize(second).replaceAll("\\s+", " ").trim());
	}

	private static boolean contains(String value, String expected) {
		return value != null && normalize(value).contains(normalize(expected));
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replace('\u2019', '\'');
	}

	private static String name(CardEntity card) {
		return card.getName() != null ? card.getName() : "card " + card.getId();
	}
}
