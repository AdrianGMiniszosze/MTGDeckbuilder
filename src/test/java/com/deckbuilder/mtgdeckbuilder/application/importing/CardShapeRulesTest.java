package com.deckbuilder.mtgdeckbuilder.application.importing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Card shape rules")
class CardShapeRulesTest {

	@Nested
	@DisplayName("Type lines")
	class TypeLines {

		@Test
		@DisplayName("Should split supertype, type and subtype")
		void shouldSplitTypeLine() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("Legendary Artifact Creature \u2014 Golem Wizard");

			assertThat(parts.supertype()).isEqualTo("Legendary");
			assertThat(parts.type()).isEqualTo("Artifact Creature");
			assertThat(parts.subtype()).isEqualTo("Golem Wizard");
		}

		@Test
		@DisplayName("Should keep every recognized supertype")
		void shouldKeepEverySupertype() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("Basic Snow Land \u2014 Mountain");

			assertThat(parts.supertype()).isEqualTo("Basic Snow");
			assertThat(parts.type()).isEqualTo("Land");
			assertThat(parts.subtype()).isEqualTo("Mountain");
		}

		@Test
		@DisplayName("Should accept a type line without subtypes")
		void shouldAcceptTypeLineWithoutSubtype() {
			final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine("Instant");

			assertThat(parts.supertype()).isEmpty();
			assertThat(parts.type()).isEqualTo("Instant");
			assertThat(parts.subtype()).isEmpty();
		}

		@Test
		@DisplayName("Should ignore unknown tokens")
		void shouldIgnoreUnknownTokens() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("Token Creature \u2014 Wolf");

			assertThat(parts.supertype()).isEmpty();
			assertThat(parts.type()).isEqualTo("Creature");
			assertThat(parts.subtype()).isEqualTo("Wolf");
		}

		@Test
		@DisplayName("Should canonicalize lower-case tokens")
		void shouldCanonicalizeCase() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("legendary creature \u2014 goblin wizard");

			assertThat(parts.supertype()).isEqualTo("Legendary");
			assertThat(parts.type()).isEqualTo("Creature");
			assertThat(parts.subtype()).isEqualTo("goblin wizard");
		}

		@Test
		@DisplayName("Should keep dashes inside the subtype")
		void shouldKeepDashesInsideSubtype() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("Creature \u2014 Assembly-Worker");

			assertThat(parts.subtype()).isEqualTo("Assembly-Worker");
		}

		@Test
		@DisplayName("Should keep an em dash inside the subtype")
		void shouldKeepEmDashInsideSubtype() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("Creature \u2014 A \u2014 B");

			assertThat(parts.subtype()).isEqualTo("A \u2014 B");
		}

		@Test
		@DisplayName("Should return empty parts for a null type line")
		void shouldReturnEmptyPartsForNull() {
			final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine(null);

			assertThat(parts.supertype()).isEmpty();
			assertThat(parts.type()).isEmpty();
			assertThat(parts.subtype()).isEmpty();
		}

		@Test
		@DisplayName("Should return empty parts for a blank type line")
		void shouldReturnEmptyPartsForBlank() {
			final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine("   ");

			assertThat(parts.supertype()).isEmpty();
			assertThat(parts.type()).isEmpty();
			assertThat(parts.subtype()).isEmpty();
		}

		@Test
		@DisplayName("Should accept a type line that starts with the separator")
		void shouldAcceptLeadingSeparator() {
			final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine("\u2014 Wolf");

			assertThat(parts.supertype()).isEmpty();
			assertThat(parts.type()).isEmpty();
			assertThat(parts.subtype()).isEqualTo("Wolf");
		}

		@Test
		@DisplayName("Should fall back to Unknown for a missing type line")
		void shouldFallBackToUnknownTypeLine() {
			assertThat(CardShapeRules.typeLine(null)).isEqualTo(CardShapeRules.UNKNOWN_TYPE_LINE);
			assertThat(CardShapeRules.typeLine(" ")).isEqualTo(CardShapeRules.UNKNOWN_TYPE_LINE);
		}

		@Test
		@DisplayName("Should keep a present type line")
		void shouldKeepPresentTypeLine() {
			assertThat(CardShapeRules.typeLine("Creature \u2014 Bear")).isEqualTo("Creature \u2014 Bear");
		}

		@Test
		@DisplayName("Should fall back to Unknown for a missing card type")
		void shouldFallBackToUnknownCardType() {
			final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine("");

			assertThat(CardShapeRules.cardType(parts)).isEqualTo(CardShapeRules.UNKNOWN_CARD_TYPE);
		}

		@Test
		@DisplayName("Should use the parsed card type when present")
		void shouldUseParsedCardType() {
			final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine("Legendary Creature \u2014 Bear");

			assertThat(CardShapeRules.cardType(parts)).isEqualTo("Creature");
		}
	}

	@Nested
	@DisplayName("Mana value")
	class ManaValue {

		@Test
		@DisplayName("Should map a missing mana value to zero")
		void shouldMapMissingToZero() {
			assertThat(CardShapeRules.manaValue(null)).isZero();
		}

		@Test
		@DisplayName("Should keep an integer mana value")
		void shouldKeepInteger() {
			assertThat(CardShapeRules.manaValue(3)).isEqualTo(3);
		}

		@Test
		@DisplayName("Should truncate a decimal mana value")
		void shouldTruncateDecimal() {
			assertThat(CardShapeRules.manaValue(2.9d)).isEqualTo(2);
			assertThat(CardShapeRules.manaValue(new BigDecimal("4.5"))).isEqualTo(4);
		}
	}

	@Nested
	@DisplayName("Images")
	class Images {

		@Test
		@DisplayName("Should use a present image URL")
		void shouldUsePresentImage() {
			assertThat(CardShapeRules.imageUrl("https://img.example/card.jpg"))
				.isEqualTo("https://img.example/card.jpg");
		}

		@Test
		@DisplayName("Should use the placeholder for a null image")
		void shouldUsePlaceholderForNull() {
			assertThat(CardShapeRules.imageUrl(null)).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
		}

		@Test
		@DisplayName("Should use the placeholder for a blank image")
		void shouldUsePlaceholderForBlank() {
			assertThat(CardShapeRules.imageUrl(" ")).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
		}
	}

	@Nested
	@DisplayName("Basic lands")
	class BasicLands {

		@Test
		@DisplayName("Should detect a basic land")
		void shouldDetectBasicLand() {
			final CardShapeRules.TypeParts parts = CardShapeRules
				.parseTypeLine("Basic Land \u2014 Mountain");

			assertThat(CardShapeRules.isBasicLand(parts)).isTrue();
		}

		@Test
		@DisplayName("Should reject a basic non-land")
		void shouldRejectBasicNonLand() {
			final CardShapeRules.TypeParts parts = new CardShapeRules.TypeParts("Basic", "Creature", "");

			assertThat(CardShapeRules.isBasicLand(parts)).isFalse();
		}

		@Test
		@DisplayName("Should reject a non-basic land")
		void shouldRejectNonBasicLand() {
			final CardShapeRules.TypeParts parts = new CardShapeRules.TypeParts("Legendary", "Land", "");

			assertThat(CardShapeRules.isBasicLand(parts)).isFalse();
		}

		@Test
		@DisplayName("Should reject empty parts")
		void shouldRejectEmptyParts() {
			assertThat(CardShapeRules.isBasicLand(new CardShapeRules.TypeParts("", "", ""))).isFalse();
		}
	}
}
