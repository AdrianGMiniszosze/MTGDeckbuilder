package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Scryfall card mapper")
class ScryfallCardMapperTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final UUID SCRYFALL_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
	private static final UUID ORACLE_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

	@Nested
	@DisplayName("Skipped rows")
	class SkippedRows {

		@Test
		@DisplayName("Should skip a null node")
		void shouldSkipNull() {
			assertThat(ScryfallCardMapper.toImportCard(null)).isEmpty();
		}

		@Test
		@DisplayName("Should skip a non-object node")
		void shouldSkipArray() {
			assertThat(ScryfallCardMapper.toImportCard(json("[1, 2]"))).isEmpty();
		}

		@Test
		@DisplayName("Should skip tokens")
		void shouldSkipToken() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Soldier","layout":"token"}
					"""))).isEmpty();
		}

		@Test
		@DisplayName("Should skip emblems")
		void shouldSkipEmblem() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Emblem","layout":"emblem"}
					"""))).isEmpty();
		}

		@Test
		@DisplayName("Should skip art series")
		void shouldSkipArtSeries() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Art","layout":"art_series"}
					"""))).isEmpty();
		}

		@Test
		@DisplayName("Should skip a row without a printing id")
		void shouldSkipMissingId() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"name":"No id","layout":"normal"}
					"""))).isEmpty();
		}

		@Test
		@DisplayName("Should skip a row with an invalid printing id")
		void shouldSkipInvalidId() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"id":"not-a-uuid","name":"Bad id","layout":"normal"}
					"""))).isEmpty();
		}

		@Test
		@DisplayName("Should skip a row without a name")
		void shouldSkipMissingName() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"id":"11111111-1111-4111-8111-111111111111","layout":"normal"}
					"""))).isEmpty();
		}

		@Test
		@DisplayName("Should skip a row with a blank name")
		void shouldSkipBlankName() {
			assertThat(ScryfallCardMapper.toImportCard(json("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"   ","layout":"normal"}
					"""))).isEmpty();
		}
	}

	@Nested
	@DisplayName("Card rows")
	class CardRows {

		@Test
		@DisplayName("Should map every card-level field")
		void shouldMapEveryField() {
			final ImportCard card = map("""
					{
					  "id":"11111111-1111-4111-8111-111111111111",
					  "oracle_id":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
					  "name":"Test Card",
					  "mana_cost":"{1}{G}",
					  "cmc":2.9,
					  "color_identity":["G","W"],
					  "type_line":"Legendary Creature — Elf",
					  "oracle_text":"A deck can have up to seven cards named Test Card.",
					  "rarity":"mythic",
					  "flavor_text":"Flavor",
					  "power":"2",
					  "toughness":"3",
					  "image_uris":{"normal":"https://img.example/test.jpg"},
					  "lang":"es",
					  "collector_number":"7",
					  "foil":true,
					  "game_changer":true,
					  "promo":true,
					  "variation":true,
					  "layout":"normal"
					}
					""");

			assertThat(card.scryfallId()).isEqualTo(SCRYFALL_ID);
			assertThat(card.oracleId()).isEqualTo(ORACLE_ID);
			assertThat(card.name()).isEqualTo("Test Card");
			assertThat(card.manaCost()).isEqualTo("{1}{G}");
			assertThat(card.cmc()).isEqualTo(2);
			assertThat(card.colorIdentity()).isEqualTo("G,W");
			assertThat(card.typeLine()).isEqualTo("Legendary Creature — Elf");
			assertThat(card.cardType()).isEqualTo("Creature");
			assertThat(card.cardSupertype()).isEqualTo("Legendary");
			assertThat(card.rarity()).isEqualTo("mythic");
			assertThat(card.cardText()).isEqualTo("A deck can have up to seven cards named Test Card.");
			assertThat(card.flavorText()).isEqualTo("Flavor");
			assertThat(card.power()).isEqualTo("2");
			assertThat(card.toughness()).isEqualTo("3");
			assertThat(card.copiesLimit()).isEqualTo(7);
			assertThat(card.basicLand()).isFalse();
			assertThat(card.imageUrl()).isEqualTo("https://img.example/test.jpg");
			assertThat(card.foil()).isTrue();
			assertThat(card.gameChanger()).isTrue();
			assertThat(card.language()).isEqualTo("es");
			assertThat(card.collectorNumber()).isEqualTo("7");
			assertThat(card.promo()).isTrue();
			assertThat(card.variation()).isTrue();
		}

		@Test
		@DisplayName("Should apply fallbacks for a minimal row")
		void shouldApplyFallbacks() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Minimal","layout":"normal"}
					""");

			assertThat(card.oracleId()).isNull();
			assertThat(card.manaCost()).isNull();
			assertThat(card.cmc()).isZero();
			assertThat(card.colorIdentity()).isNull();
			assertThat(card.typeLine()).isEqualTo(CardShapeRules.UNKNOWN_TYPE_LINE);
			assertThat(card.cardType()).isEqualTo(CardShapeRules.UNKNOWN_CARD_TYPE);
			assertThat(card.cardSupertype()).isNull();
			assertThat(card.rarity()).isEqualTo("common");
			assertThat(card.cardText()).isEmpty();
			assertThat(card.flavorText()).isNull();
			assertThat(card.power()).isNull();
			assertThat(card.toughness()).isNull();
			assertThat(card.copiesLimit()).isNull();
			assertThat(card.basicLand()).isFalse();
			assertThat(card.imageUrl()).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
			assertThat(card.foil()).isFalse();
			assertThat(card.gameChanger()).isFalse();
			assertThat(card.language()).isEqualTo("en");
			assertThat(card.collectorNumber()).isEqualTo("unknown");
			assertThat(card.promo()).isFalse();
			assertThat(card.variation()).isFalse();
		}

		@Test
		@DisplayName("Should treat blank optional fields as absent")
		void shouldTreatBlankFieldsAsAbsent() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Blanks","mana_cost":" ",
					 "type_line":" ","rarity":" ","flavor_text":" ","power":" ","toughness":" ",
					 "collector_number":" ","lang":" ","language":"","layout":"normal"}
					""");

			assertThat(card.manaCost()).isNull();
			assertThat(card.typeLine()).isEqualTo(CardShapeRules.UNKNOWN_TYPE_LINE);
			assertThat(card.rarity()).isEqualTo("common");
			assertThat(card.flavorText()).isNull();
			assertThat(card.power()).isNull();
			assertThat(card.toughness()).isNull();
			assertThat(card.collectorNumber()).isEqualTo("unknown");
			assertThat(card.language()).isEqualTo("en");
		}

		@Test
		@DisplayName("Should treat explicit nulls as absent")
		void shouldTreatExplicitNullsAsAbsent() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Nulls","oracle_id":null,
					 "mana_cost":null,"cmc":null,"color_identity":null,"type_line":null,
					 "oracle_text":null,"rarity":null,"flavor_text":null,"power":null,
					 "toughness":null,"image_uris":null,"lang":null,"collector_number":null,
					 "layout":"normal"}
					""");

			assertThat(card.oracleId()).isNull();
			assertThat(card.manaCost()).isNull();
			assertThat(card.cmc()).isZero();
			assertThat(card.colorIdentity()).isNull();
			assertThat(card.typeLine()).isEqualTo(CardShapeRules.UNKNOWN_TYPE_LINE);
			assertThat(card.cardType()).isEqualTo(CardShapeRules.UNKNOWN_CARD_TYPE);
			assertThat(card.rarity()).isEqualTo("common");
			assertThat(card.cardText()).isEmpty();
			assertThat(card.copiesLimit()).isNull();
			assertThat(card.imageUrl()).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
			assertThat(card.language()).isEqualTo("en");
			assertThat(card.collectorNumber()).isEqualTo("unknown");
		}

		@Test
		@DisplayName("Should use the language fallback and then English")
		void shouldUseLanguageFallback() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Lang","language":"pt","layout":"normal"}
					""").language()).isEqualTo("pt");
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Lang","lang":"ja","language":"pt","layout":"normal"}
					""").language()).isEqualTo("ja");
		}

		@Test
		@DisplayName("Should ignore an invalid oracle id")
		void shouldIgnoreInvalidOracleId() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","oracle_id":"bad","name":"Oracle","layout":"normal"}
					""").oracleId()).isNull();
		}

		@Test
		@DisplayName("Should ignore an empty colour identity")
		void shouldIgnoreEmptyColorIdentity() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Colors","color_identity":[],"layout":"normal"}
					""").colorIdentity()).isNull();
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Colors","color_identity":"W","layout":"normal"}
					""").colorIdentity()).isNull();
		}

		@Test
		@DisplayName("Should drop blank colours from the identity")
		void shouldDropBlankColors() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Colors","color_identity":["G"," "],"layout":"normal"}
					""").colorIdentity()).isEqualTo("G");
		}

		@Test
		@DisplayName("Should ignore a non-numeric mana value")
		void shouldIgnoreNonNumericManaValue() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Cmc","cmc":"many","layout":"normal"}
					""").cmc()).isZero();
		}

		@Test
		@DisplayName("Should fall back to the first face image")
		void shouldFallBackToFirstFaceImage() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Faces",
					 "card_faces":[{"image_uris":{"normal":"https://img.example/face.jpg"}}],"layout":"transform"}
					""").imageUrl()).isEqualTo("https://img.example/face.jpg");
		}

		@Test
		@DisplayName("Should ignore an empty or malformed face list")
		void shouldIgnoreMalformedFaces() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Faces",
					 "card_faces":[],"layout":"transform"}
					""").imageUrl()).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Faces",
					 "card_faces":"nope","layout":"transform"}
					""").imageUrl()).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
		}

		@Test
		@DisplayName("Should ignore malformed image objects")
		void shouldIgnoreMalformedImages() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Image",
					 "image_uris":"nope","layout":"normal"}
					""").imageUrl()).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Image",
					 "image_uris":{},"layout":"normal"}
					""").imageUrl()).isEqualTo(CardShapeRules.MISSING_IMAGE_URL);
		}

		@Test
		@DisplayName("Should derive an unlimited copy count and basic land flag")
		void shouldDeriveCopiesAndBasicLand() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Island",
					 "type_line":"Basic Land — Island",
					 "oracle_text":"A deck can have any number of cards named Island.","layout":"normal"}
					""");

			assertThat(card.copiesLimit()).isZero();
			assertThat(card.basicLand()).isTrue();
		}

		@Test
		@DisplayName("Should keep explicit false booleans")
		void shouldKeepFalseBooleans() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"False",
					 "foil":false,"game_changer":false,"promo":false,"variation":false,"layout":"normal"}
					""");

			assertThat(card.foil()).isFalse();
			assertThat(card.gameChanger()).isFalse();
			assertThat(card.promo()).isFalse();
			assertThat(card.variation()).isFalse();
		}
	}

	private static ImportCard map(String json) {
		final Optional<ImportCard> card = ScryfallCardMapper.toImportCard(json(json));
		assertThat(card).isPresent();
		return card.orElseThrow();
	}

	private static JsonNode json(String value) {
		try {
			return OBJECT_MAPPER.readTree(value);
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}
}
