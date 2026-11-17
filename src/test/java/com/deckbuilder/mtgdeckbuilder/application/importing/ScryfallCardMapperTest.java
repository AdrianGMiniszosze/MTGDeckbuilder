package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
					  "set_name":"Test Set",
					  "set":"tst",
					  "released_at":"2026-01-01",
					  "set_type":"expansion",
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
			assertThat(card.setName()).isEqualTo("Test Set");
			assertThat(card.setCode()).isEqualTo("tst");
			assertThat(card.setReleasedAt()).isEqualTo(LocalDate.of(2026, 1, 1));
			assertThat(card.setType()).isEqualTo("expansion");
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
			assertThat(card.setName()).isNull();
			assertThat(card.setCode()).isNull();
			assertThat(card.setReleasedAt()).isNull();
			assertThat(card.setType()).isNull();
			assertThat(card.prices()).isEmpty();
			assertThat(card.attributes().colors()).isEmpty();
			assertThat(card.attributes().colorIdentity()).isEmpty();
			assertThat(card.attributes().types()).isEmpty();
			assertThat(card.attributes().supertypes()).isEmpty();
			assertThat(card.attributes().subtypes()).isEmpty();
			assertThat(card.attributes().keywords()).isEmpty();
			assertThat(card.attributes().legalities()).isEmpty();
			assertThat(card.attributes().faces()).isEmpty();
		}

		@Test
		@DisplayName("Should treat blank optional fields as absent")
		void shouldTreatBlankFieldsAsAbsent() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Blanks","mana_cost":" ",
					 "type_line":" ","rarity":" ","flavor_text":" ","power":" ","toughness":" ",
					 "collector_number":" ","lang":" ","language":"","set_name":" ","set":" ",
					 "released_at":" ","set_type":" ","layout":"normal"}
					""");

			assertThat(card.manaCost()).isNull();
			assertThat(card.typeLine()).isEqualTo(CardShapeRules.UNKNOWN_TYPE_LINE);
			assertThat(card.rarity()).isEqualTo("common");
			assertThat(card.flavorText()).isNull();
			assertThat(card.power()).isNull();
			assertThat(card.toughness()).isNull();
			assertThat(card.collectorNumber()).isEqualTo("unknown");
			assertThat(card.language()).isEqualTo("en");
			assertThat(card.setName()).isNull();
			assertThat(card.setCode()).isNull();
			assertThat(card.setReleasedAt()).isNull();
			assertThat(card.setType()).isNull();
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
		@DisplayName("Should ignore an invalid oracle id and release date")
		void shouldIgnoreInvalidIdentifiers() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","oracle_id":"bad","name":"Oracle",
					 "released_at":"not-a-date","layout":"normal"}
					""");

			assertThat(card.oracleId()).isNull();
			assertThat(card.setReleasedAt()).isNull();
		}

		@Test
		@DisplayName("Should drop invalid colours from the identity")
		void shouldDropInvalidColors() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Colors","color_identity":["G"," ","X"],"layout":"normal"}
					""").colorIdentity()).isEqualTo("G");
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Colors","color_identity":[],"layout":"normal"}
					""").colorIdentity()).isNull();
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Colors","color_identity":"W","layout":"normal"}
					""").colorIdentity()).isNull();
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

	@Nested
	@DisplayName("Attributes")
	class Attributes {

		@Test
		@DisplayName("Should map colours, types, keywords and legalities")
		void shouldMapAttributes() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Attrs",
					 "colors":["W","U"],"color_identity":["W","U"],
					 "type_line":"Legendary Artifact Creature — Golem Wizard",
					 "keywords":["Flying","Ward"],
					 "legalities":{"standard":"legal","modern":"not_legal","commander":"legal",
					              "vintage":"restricted","pauper":"banned","legacy":"legalish",
					              "alchemy":"legal"},
					 "layout":"normal"}
					""");

			assertThat(card.attributes().colors()).containsExactly("W", "U");
			assertThat(card.attributes().colorIdentity()).containsExactly("W", "U");
			assertThat(card.attributes().types()).containsExactly("Artifact", "Creature");
			assertThat(card.attributes().supertypes()).containsExactly("Legendary");
			assertThat(card.attributes().subtypes()).containsExactly("Golem", "Wizard");
			assertThat(card.attributes().keywords()).containsExactly("Flying", "Ward");
			assertThat(card.attributes().legalities())
				.containsEntry("Standard", "legal")
				.containsEntry("Modern", "not_legal")
				.containsEntry("Commander", "legal")
				.containsEntry("Vintage", "restricted")
				.containsEntry("Pauper", "banned")
				.hasSize(5);
		}

		@Test
		@DisplayName("Should ignore malformed attribute nodes")
		void shouldIgnoreMalformedAttributes() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Bad attrs",
					 "colors":"nope","color_identity":{},"keywords":"Flying",
					 "legalities":["standard"],"layout":"normal"}
					""");

			assertThat(card.attributes().colors()).isEmpty();
			assertThat(card.attributes().colorIdentity()).isEmpty();
			assertThat(card.attributes().keywords()).isEmpty();
			assertThat(card.attributes().legalities()).isEmpty();
		}

		@Test
		@DisplayName("Should drop blank and duplicate keyword entries")
		void shouldDropBlankKeywords() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Keywords",
					 "keywords":["Flying"," ","Flying","Ward"],"layout":"normal"}
					""").attributes().keywords()).containsExactly("Flying", "Ward");
		}
	}

	@Nested
	@DisplayName("Prices")
	class Prices {

		@Test
		@DisplayName("Should map the known price fields")
		void shouldMapPrices() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Prices",
					 "prices":{"usd":"1.25","usd_foil":null,"usd_etched":"","eur":"0.80",
					           "eur_foil":"cheap","eur_etched":"-1","tix":"2","other":"9"},
					 "layout":"normal"}
					""");

			assertThat(card.prices())
				.containsEntry("usd", new BigDecimal("1.25"))
				.containsEntry("eur", new BigDecimal("0.80"))
				.containsEntry("tix", new BigDecimal("2"))
				.hasSize(3)
				.doesNotContainKeys("usd_foil", "usd_etched", "eur_foil", "eur_etched", "other");
		}

		@Test
		@DisplayName("Should ignore a missing or malformed price node")
		void shouldIgnoreMalformedPrices() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"No prices","layout":"normal"}
					""").prices()).isEmpty();
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"No prices",
					 "prices":"nope","layout":"normal"}
					""").prices()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Faces")
	class Faces {

		@Test
		@DisplayName("Should map every face field and its raw object")
		void shouldMapFaces() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Double // Face",
					 "cmc":2,"type_line":"Creature — Human // Creature — Insect",
					 "card_faces":[
					   {"name":"Front","mana_cost":"{U}","type_line":"Creature — Human Wizard",
					    "oracle_text":"Front text.","colors":["U"],"power":"1","toughness":"1",
					    "loyalty":"3","defense":"4","image_uris":{"normal":"https://img.example/front.jpg"}},
					   {"name":"Back","type_line":"Creature — Human Insect","oracle_text":"Flying.",
					    "color_indicator":["U"],"colors":["U"," "],"power":"3","toughness":"2"}
					 ],
					 "layout":"transform"}
					""");

			assertThat(card.attributes().faces()).hasSize(2);
			final ImportFace front = card.attributes().faces().get(0);
			assertThat(front.name()).isEqualTo("Front");
			assertThat(front.manaCost()).isEqualTo("{U}");
			assertThat(front.typeLine()).isEqualTo("Creature — Human Wizard");
			assertThat(front.oracleText()).isEqualTo("Front text.");
			assertThat(front.colors()).containsExactly("U");
			assertThat(front.power()).isEqualTo("1");
			assertThat(front.toughness()).isEqualTo("1");
			assertThat(front.loyalty()).isEqualTo("3");
			assertThat(front.defense()).isEqualTo("4");
			assertThat(front.imageUris().get("normal").asText()).isEqualTo("https://img.example/front.jpg");
			assertThat(front.faceData().get("name").asText()).isEqualTo("Front");

			final ImportFace back = card.attributes().faces().get(1);
			assertThat(back.manaCost()).isEmpty();
			assertThat(back.colors()).containsExactly("U");
			assertThat(back.colorIndicator()).containsExactly("U");
			assertThat(back.imageUris()).isNull();

			assertThat(card.attributes().types()).containsExactly("Creature");
			assertThat(card.attributes().subtypes()).containsExactly("Human", "Wizard", "Insect");
		}

		@Test
		@DisplayName("Should join face text and derive the copy limit from it")
		void shouldJoinFaceText() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Join",
					 "card_faces":[
					   {"name":"Front","type_line":"Creature — Human","oracle_text":"First."},
					   {"name":"Back","type_line":"Creature — Insect",
					    "oracle_text":"A deck can have any number of cards named Join."}
					 ],"layout":"transform"}
					""");

			assertThat(card.cardText()).isEqualTo("First.\n\nA deck can have any number of cards named Join.");
			assertThat(card.copiesLimit()).isZero();
		}

		@Test
		@DisplayName("Should fall back to the card name and default mana cost")
		void shouldFallBackFaceFields() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Fallback",
					 "card_faces":[{"type_line":"Creature — Bear"}],"layout":"transform"}
					""");

			final ImportFace face = card.attributes().faces().get(0);
			assertThat(face.name()).isEqualTo("Fallback");
			assertThat(face.manaCost()).isEmpty();
			assertThat(face.colorIndicator()).isEmpty();
		}

		@Test
		@DisplayName("Should skip malformed face entries and use the top-level type line")
		void shouldSkipMalformedFaces() {
			final ImportCard card = map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Malformed",
					 "type_line":"Legendary Creature — Elf",
					 "card_faces":["nope",{}],"layout":"transform"}
					""");

			assertThat(card.attributes().faces()).hasSize(1);
			assertThat(card.attributes().faces().get(0).name()).isEqualTo("Malformed");
			assertThat(card.cardType()).isEqualTo("Creature");
			assertThat(card.cardSupertype()).isEqualTo("Legendary");
			assertThat(card.attributes().subtypes()).containsExactly("Elf");
		}

		@Test
		@DisplayName("Should not create faces for a single-face card")
		void shouldNotCreateFaces() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Single",
					 "type_line":"Creature — Bear","layout":"normal"}
					""").attributes().faces()).isEmpty();
		}

		@Test
		@DisplayName("Should use the first face image when the card has none")
		void shouldUseFirstFaceImage() {
			assertThat(map("""
					{"id":"11111111-1111-4111-8111-111111111111","name":"Image",
					 "card_faces":[{"image_uris":{"normal":"https://img.example/f.jpg"}}],"layout":"transform"}
					""").imageUrl()).isEqualTo("https://img.example/f.jpg");
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
