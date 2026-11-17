package com.deckbuilder.mtgdeckbuilder.application.importing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Legality rules")
class LegalityRulesTest {

	@Nested
	@DisplayName("Format names")
	class FormatNames {

		@Test
		@DisplayName("Should map every seeded Scryfall key")
		void shouldMapSeededKeys() {
			assertThat(LegalityRules.formatName("standard")).contains("Standard");
			assertThat(LegalityRules.formatName("modern")).contains("Modern");
			assertThat(LegalityRules.formatName("legacy")).contains("Legacy");
			assertThat(LegalityRules.formatName("vintage")).contains("Vintage");
			assertThat(LegalityRules.formatName("pauper")).contains("Pauper");
			assertThat(LegalityRules.formatName("commander")).contains("Commander");
		}

		@Test
		@DisplayName("Should trim and ignore case")
		void shouldTrimAndIgnoreCase() {
			assertThat(LegalityRules.formatName("  CoMmAnDeR ")).contains("Commander");
		}

		@Test
		@DisplayName("Should ignore keys without a seeded format")
		void shouldIgnoreUnknownKey() {
			assertThat(LegalityRules.formatName("alchemy")).isEmpty();
		}

		@Test
		@DisplayName("Should reject a null key")
		void shouldRejectNullKey() {
			assertThat(LegalityRules.formatName(null)).isEmpty();
		}

		@Test
		@DisplayName("Should reject a blank key")
		void shouldRejectBlankKey() {
			assertThat(LegalityRules.formatName("   ")).isEmpty();
		}
	}

	@Nested
	@DisplayName("Statuses")
	class Statuses {

		@Test
		@DisplayName("Should accept every status stored by card_legality")
		void shouldAcceptKnownStatuses() {
			assertThat(LegalityRules.status("legal")).contains("legal");
			assertThat(LegalityRules.status("not_legal")).contains("not_legal");
			assertThat(LegalityRules.status("restricted")).contains("restricted");
			assertThat(LegalityRules.status("banned")).contains("banned");
		}

		@Test
		@DisplayName("Should trim and normalize case")
		void shouldNormalizeCase() {
			assertThat(LegalityRules.status("  BANNED ")).contains("banned");
		}

		@Test
		@DisplayName("Should ignore an unknown status")
		void shouldIgnoreUnknownStatus() {
			assertThat(LegalityRules.status("unavailable")).isEmpty();
		}

		@Test
		@DisplayName("Should reject a null status")
		void shouldRejectNullStatus() {
			assertThat(LegalityRules.status(null)).isEmpty();
		}

		@Test
		@DisplayName("Should reject a blank status")
		void shouldRejectBlankStatus() {
			assertThat(LegalityRules.status("   ")).isEmpty();
		}
	}
}
