package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Commander Rules Tests")
class CommanderRulesTest {

	@Nested
	@DisplayName("Standalone commanders")
	class Standalone {

		@Test
		@DisplayName("Should accept a legendary creature")
		void shouldAcceptLegendaryCreature() {
			final CardEntity card = card("Krav, the Unredeemed", "Legendary Creature — Demon", null);

			assertThat(CommanderRules.isCommanderOnItsOwn(card)).isTrue();
			assertThat(CommanderRules.validateAlone(card)).isNull();
		}

		@Test
		@DisplayName("Should accept a card that says it can be the commander")
		void shouldAcceptCardThatSaysItCanBeCommander() {
			final CardEntity card = card("Teferi, Temporal Archmage", "Legendary Planeswalker — Teferi",
					"Teferi, Temporal Archmage can be your commander.");

			assertThat(CommanderRules.isCommanderOnItsOwn(card)).isTrue();
			assertThat(CommanderRules.validateAlone(card)).isNull();
		}

		@Test
		@DisplayName("Should reject a non-legendary card without the permission text")
		void shouldRejectPlainCard() {
			final CardEntity card = card("Grizzly Bears", "Creature — Bear", null);

			assertThat(CommanderRules.isCommanderOnItsOwn(card)).isFalse();
			assertThat(CommanderRules.validateAlone(card)).contains("cannot be a commander");
		}

		@Test
		@DisplayName("Should reject a Background on its own")
		void shouldRejectBackgroundAlone() {
			final CardEntity card = card("Folk Hero", "Legendary Enchantment — Background", null);
			card.setSubtypes(List.of("Background"));

			assertThat(CommanderRules.isBackground(card)).isTrue();
			assertThat(CommanderRules.isCommanderOnItsOwn(card)).isFalse();
			assertThat(CommanderRules.validateAlone(card)).contains("Background");
		}

		@Test
		@DisplayName("Should fall back to the card id when the card has no name")
		void shouldFallBackToIdWhenUnnamed() {
			final CardEntity card = card(null, "Creature", null);
			card.setId(7L);

			assertThat(CommanderRules.validateAlone(card)).contains("card 7");
		}
	}

	@Nested
	@DisplayName("Partner variants")
	class Partners {

		@Test
		@DisplayName("Should accept a matching Partner with pair")
		void shouldAcceptPartnerWithPair() {
			final CardEntity krav = card("Krav, the Unredeemed", "Legendary Creature — Demon",
					"Partner with Regna, the Redeemer");
			final CardEntity regna = card("Regna, the Redeemer", "Legendary Creature — Angel",
					"Partner with Krav, the Unredeemed");

			assertThat(CommanderRules.partnerWithName(krav)).contains("Regna, the Redeemer");
			assertThat(CommanderRules.isPartnerWithPair(krav, regna)).isTrue();
			assertThat(CommanderRules.validatePair(krav, regna)).isNull();
		}

		@Test
		@DisplayName("Should reject Partner with names that do not match")
		void shouldRejectPartnerWithMismatch() {
			final CardEntity krav = card("Krav, the Unredeemed", "Legendary Creature — Demon",
					"Partner with Someone Else");
			final CardEntity regna = card("Regna, the Redeemer", "Legendary Creature — Angel",
					"Partner with Krav, the Unredeemed");

			assertThat(CommanderRules.isPartnerWithPair(krav, regna)).isFalse();
			assertThat(CommanderRules.validatePair(krav, regna)).isNotNull();
		}

		@Test
		@DisplayName("Should accept two generic partners")
		void shouldAcceptGenericPartners() {
			final CardEntity first = keyworded(card("Thrasios, Triton Hero", "Legendary Creature — Human", null),
					"Partner");
			final CardEntity second = keyworded(card("Tymna the Weaver", "Legendary Creature — Human Cleric", null),
					"Partner");

			assertThat(CommanderRules.isGenericPartnerPair(first, second)).isTrue();
			assertThat(CommanderRules.validatePair(first, second)).isNull();
		}

		@Test
		@DisplayName("Should reject a generic partner and a Partner with card")
		void shouldRejectMixedPartnerKinds() {
			final CardEntity generic = keyworded(card("Tymna the Weaver", "Legendary Creature — Human", null),
					"Partner");
			final CardEntity partnerWith = card("Regna, the Redeemer", "Legendary Creature — Angel",
					"Partner with Krav, the Unredeemed");

			assertThat(CommanderRules.isGenericPartnerPair(generic, partnerWith)).isFalse();
		}

		@Test
		@DisplayName("Should accept Friends forever")
		void shouldAcceptFriendsForever() {
			final CardEntity first = card("Will, Scion of Peace", "Legendary Creature — Human", "Friends forever");
			final CardEntity second = keyworded(
					card("Rowan, Scion of War", "Legendary Creature — Human Wizard", null), "Friends forever");

			assertThat(CommanderRules.isFriendsForeverPair(first, second)).isTrue();
			assertThat(CommanderRules.validatePair(first, second)).isNull();
		}

		@Test
		@DisplayName("Should require Friends forever on both cards")
		void shouldRequireFriendsForeverOnBoth() {
			final CardEntity first = card("Will, Scion of Peace", "Legendary Creature — Human", "Friends forever");
			final CardEntity second = card("Random Legend", "Legendary Creature — Human", null);

			assertThat(CommanderRules.isFriendsForeverPair(first, second)).isFalse();
		}

		@Test
		@DisplayName("Should accept Choose a Background with a Background")
		void shouldAcceptBackgroundPair() {
			final CardEntity commander = keyworded(
					card("Burakos, Party Leader", "Legendary Creature — Orc", null), "Choose a Background");
			final CardEntity background = card("Folk Hero", "Legendary Enchantment — Background", null);
			background.setSubtypes(List.of("Background"));

			assertThat(CommanderRules.isBackgroundPair(commander, background)).isTrue();
			assertThat(CommanderRules.isBackgroundPair(background, commander)).isTrue();
			assertThat(CommanderRules.validatePair(commander, background)).isNull();
		}

		@Test
		@DisplayName("Should reject Choose a Background with a non-Background")
		void shouldRejectBackgroundPairWithNonBackground() {
			final CardEntity commander = keyworded(
					card("Burakos, Party Leader", "Legendary Creature — Orc", null), "Choose a Background");
			final CardEntity other = card("Random Legend", "Legendary Creature — Human", null);

			assertThat(CommanderRules.isBackgroundPair(commander, other)).isFalse();
		}

		@Test
		@DisplayName("Should accept Doctor's companion with a Doctor")
		void shouldAcceptDoctorsCompanionPair() {
			final CardEntity companion = card("Clara Oswald", "Legendary Creature — Human",
					"Doctor's companion");
			final CardEntity doctor = doctor();

			assertThat(CommanderRules.isDoctor(doctor)).isTrue();
			assertThat(CommanderRules.isDoctorsCompanionPair(companion, doctor)).isTrue();
			assertThat(CommanderRules.isDoctorsCompanionPair(doctor, companion)).isTrue();
			assertThat(CommanderRules.validatePair(companion, doctor)).isNull();
		}

		@Test
		@DisplayName("Should reject Doctor's companion without a Doctor")
		void shouldRejectDoctorsCompanionWithoutDoctor() {
			final CardEntity companion = card("Clara Oswald", "Legendary Creature — Human", "Doctor's companion");
			final CardEntity other = card("Random Legend", "Legendary Creature — Human", null);

			assertThat(CommanderRules.isDoctorsCompanionPair(companion, other)).isFalse();
		}

		@Test
		@DisplayName("Should reject a pair without any pairing mechanic")
		void shouldRejectPairWithoutMechanic() {
			final CardEntity first = card("Legend One", "Legendary Creature — Human", null);
			final CardEntity second = card("Legend Two", "Legendary Creature — Elf", null);

			assertThat(CommanderRules.validatePair(first, second))
				.isEqualTo("Legend One and Legend Two cannot be commanders together.");
		}

		@Test
		@DisplayName("Should reject a Partner with card against an unnamed card")
		void shouldRejectPartnerWithAgainstUnnamedCard() {
			final CardEntity first = card("A", "Legendary Creature — Human", "Partner with B");
			final CardEntity second = card(null, "Legendary Creature — Human", "Partner with A");

			assertThat(CommanderRules.partnerWithName(first)).contains("B");
			assertThat(CommanderRules.isPartnerWithPair(first, second)).isFalse();
		}

		@Test
		@DisplayName("Should ignore a Partner with card without a name")
		void shouldIgnorePartnerWithWithoutName() {
			final CardEntity card = card("A", "Legendary Creature — Human", "Partner with");

			assertThat(CommanderRules.partnerWithName(card)).isEmpty();
			assertThat(CommanderRules.isPartnerWithPair(card, card)).isFalse();
		}
	}

	@Nested
	@DisplayName("Keywords and text")
	class Keywords {

		@Test
		@DisplayName("Should read keywords when the text does not include the phrase")
		void shouldReadKeywordsOnly() {
			final CardEntity card = keyworded(card("Keyworded", "Legendary Creature — Human", null),
					"Friends forever");

			assertThat(CommanderRules.isFriendsForeverPair(card, card)).isTrue();
		}

		@Test
		@DisplayName("Should fall back to text when keywords are missing")
		void shouldFallBackToText() {
			final CardEntity card = card("Text Only", "Legendary Creature — Human", "PARTNER");
			card.setKeywords(List.of("Vigilance"));

			assertThat(CommanderRules.isGenericPartnerPair(card, card)).isTrue();

			card.setKeywords(null);
			assertThat(CommanderRules.isGenericPartnerPair(card, card)).isTrue();
		}

		@Test
		@DisplayName("Should treat a card with no data as a plain, colorless card")
		void shouldHandleEmptyCard() {
			final CardEntity blank = new CardEntity();

			assertThat(CommanderRules.isCommanderOnItsOwn(blank)).isFalse();
			assertThat(CommanderRules.isBackground(blank)).isFalse();
			assertThat(CommanderRules.isDoctor(blank)).isFalse();
			assertThat(CommanderRules.isGenericPartnerPair(blank, blank)).isFalse();
			assertThat(CommanderRules.isFriendsForeverPair(blank, blank)).isFalse();
			assertThat(CommanderRules.colorIdentity(blank)).isEmpty();
		}

		@Test
		@DisplayName("Should require the right type for Backgrounds and Doctors")
		void shouldRequireTheRightTypeForSubtypes() {
			final CardEntity backgroundCreature = card("Odd Background", "Legendary Creature — Background", null);
			backgroundCreature.setSubtypes(List.of("Background"));

			final CardEntity doctorEnchantment = card("Odd Doctor", "Legendary Enchantment — Doctor", null);
			doctorEnchantment.setSubtypes(List.of("Doctor"));

			assertThat(CommanderRules.isBackground(backgroundCreature)).isFalse();
			assertThat(CommanderRules.isDoctor(doctorEnchantment)).isFalse();
		}

		@Test
		@DisplayName("Should tolerate null keyword and subtype entries")
		void shouldTolerateNullEntries() {
			final CardEntity card = card("Nulls", "Legendary Creature — Human", null);
			card.setKeywords(Arrays.asList("Partner", null));
			card.setSubtypes(new ArrayList<>(Arrays.asList(null, "Doctor")));

			assertThat(CommanderRules.isGenericPartnerPair(card, card)).isTrue();
			assertThat(CommanderRules.isDoctor(card)).isTrue();
		}

		@Test
		@DisplayName("Should normalize typographic apostrophes")
		void shouldNormalizeApostrophes() {
			final CardEntity card = card("Clara Oswald", "Legendary Creature — Human",
					"Doctor\u2019s companion");

			assertThat(CommanderRules.isDoctorsCompanionPair(card, doctor())).isTrue();
		}

		@Test
		@DisplayName("Should trim punctuation and whitespace from Partner with names")
		void shouldTrimPartnerName() {
			final CardEntity card = card("A", "Legendary Creature — Human", "PARTNER WITH  B, the Great. ");

			assertThat(CommanderRules.partnerWithName(card)).contains("B, the Great");
		}
	}

	@Nested
	@DisplayName("Color identity")
	class Colors {

		@Test
		@DisplayName("Should parse a comma separated identity")
		void shouldParseIdentity() {
			final CardEntity card = card("Colors", "Creature", null);
			card.setColorIdentity(" w, U ");

			assertThat(CommanderRules.colorIdentity(card)).containsExactly("U", "W");
		}

		@Test
		@DisplayName("Should treat a missing identity as colorless")
		void shouldTreatMissingIdentityAsColorless() {
			final CardEntity card = card("Colorless", "Artifact", null);

			card.setColorIdentity(null);
			assertThat(CommanderRules.colorIdentity(card)).isEmpty();

			card.setColorIdentity("  ");
			assertThat(CommanderRules.colorIdentity(card)).isEmpty();

			card.setColorIdentity(",");
			assertThat(CommanderRules.colorIdentity(card)).isEmpty();
		}
	}

	private static CardEntity doctor() {
		final CardEntity doctor = card("The Fourth Doctor", "Legendary Creature — Time Lord", null);
		doctor.setSubtypes(List.of("Doctor"));
		return doctor;
	}

	private static CardEntity card(String name, String typeLine, String text) {
		final CardEntity card = new CardEntity();
		card.setName(name);
		card.setTypeLine(typeLine);
		card.setCardText(text);
		card.setKeywords(List.of());
		card.setSubtypes(List.of());
		return card;
	}

	private static CardEntity keyworded(CardEntity card, String keyword) {
		card.setKeywords(List.of(keyword));
		return card;
	}
}
