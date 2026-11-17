package com.deckbuilder.mtgdeckbuilder.application.importing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Copy limit rules")
class CopyLimitRulesTest {

	@Nested
	@DisplayName("Missing text")
	class MissingText {

		@Test
		@DisplayName("Should defer to the format when the text is null")
		void shouldReturnNullForNullText() {
			assertThat(CopyLimitRules.copiesLimit(null)).isNull();
		}

		@Test
		@DisplayName("Should defer to the format when the text is blank")
		void shouldReturnNullForBlankText() {
			assertThat(CopyLimitRules.copiesLimit("   ")).isNull();
		}
	}

	@Nested
	@DisplayName("Any number")
	class AnyNumber {

		@Test
		@DisplayName("Should map any number to zero")
		void shouldMapAnyNumberToZero() {
			assertThat(CopyLimitRules.copiesLimit(
					"A deck can have any number of cards named Persistent Petitioners."))
				.isZero();
		}

		@Test
		@DisplayName("Should ignore case")
		void shouldIgnoreCase() {
			assertThat(CopyLimitRules.copiesLimit(
					"A DECK CAN HAVE ANY NUMBER OF CARDS NAMED Relentless Rats."))
				.isZero();
		}

		@Test
		@DisplayName("Should prefer any number over an up-to sentence")
		void shouldPreferAnyNumber() {
			assertThat(CopyLimitRules.copiesLimit(
					"A deck can have any number of cards named X. A deck can have up to seven cards named X."))
				.isZero();
		}
	}

	@Nested
	@DisplayName("Exact caps")
	class ExactCaps {

		@Test
		@DisplayName("Should map a worded cap")
		void shouldMapWordedCap() {
			assertThat(CopyLimitRules.copiesLimit("A deck can have up to seven cards named Seven Dwarves."))
				.isEqualTo(7);
		}

		@Test
		@DisplayName("Should accept the singular card form")
		void shouldAcceptSingularCard() {
			assertThat(CopyLimitRules.copiesLimit("A deck can have up to one card named Lutri, the Spellchaser."))
				.isEqualTo(1);
		}

		@Test
		@DisplayName("Should map a numeric cap")
		void shouldMapNumericCap() {
			assertThat(CopyLimitRules.copiesLimit("A deck can have up to 12 cards named Rat Colony."))
				.isEqualTo(12);
		}

		@Test
		@DisplayName("Should ignore case in the construction sentence")
		void shouldIgnoreCase() {
			assertThat(CopyLimitRules.copiesLimit("a deck can have up to three cards named Nazgul."))
				.isEqualTo(3);
		}
	}

	@Nested
	@DisplayName("Ignored text")
	class IgnoredText {

		@Test
		@DisplayName("Should ignore tutor text without the deck construction prefix")
		void shouldIgnoreTutorText() {
			assertThat(CopyLimitRules.copiesLimit("Search your library for up to two cards named X.")).isNull();
		}

		@Test
		@DisplayName("Should ignore unrelated text")
		void shouldIgnoreUnrelatedText() {
			assertThat(CopyLimitRules.copiesLimit("Flying. When this creature enters, draw a card.")).isNull();
		}

		@Test
		@DisplayName("Should ignore worded numbers outside the supported list")
		void shouldIgnoreUnsupportedWord() {
			assertThat(CopyLimitRules.copiesLimit("A deck can have up to twenty cards named X.")).isNull();
		}

		@Test
		@DisplayName("Should reject a zero cap, which would mean unlimited")
		void shouldRejectZeroCap() {
			assertThat(CopyLimitRules.copiesLimit("A deck can have up to 0 cards named X.")).isNull();
		}

		@Test
		@DisplayName("Should ignore a cap that does not fit in an int")
		void shouldIgnoreOverflowingCap() {
			assertThat(CopyLimitRules.copiesLimit("A deck can have up to 99999999999 cards named X.")).isNull();
		}
	}
}
