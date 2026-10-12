package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardFaceEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardRulingEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardFace;
import com.deckbuilder.mtgdeckbuilder.model.CardRuling;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CardImportEntityMapperTest {
	private final CardEntityMapper mapper = Mappers.getMapper(CardEntityMapper.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void mapsImportPricesAndFacesToCardEntity() throws Exception {
		var faceData = objectMapper.readTree("{\"name\":\"Front\",\"oracle_text\":\"Draw a card.\"}");
		var colors = objectMapper.readTree("[\"U\"]");
		var colorIndicator = objectMapper.readTree("[\"U\"]");
		var imageUris = objectMapper.readTree("{\"normal\":\"https://example.test/card.jpg\"}");
		Card model = Card.builder()
				.usd(new BigDecimal("1.2500"))
				.usdFoil(new BigDecimal("2.2500"))
				.usdEtched(new BigDecimal("3.2500"))
				.eur(new BigDecimal("1.1500"))
				.eurFoil(new BigDecimal("2.1500"))
				.eurEtched(new BigDecimal("3.1500"))
				.tix(new BigDecimal("0.5000"))
				.priceObservedAt(OffsetDateTime.parse("2026-10-12T12:00:00Z"))
				.faces(List.of(CardFace.builder()
							.faceIndex((short) 0)
							.name("Front")
							.manaCost("{U}")
							.typeLine("Creature — Wizard")
							.oracleText("Draw a card.")
							.colors(colors)
							.colorIndicator(colorIndicator)
							.power("1")
							.toughness("2")
							.loyalty("3")
							.defense("4")
							.imageUris(imageUris)
							.faceData(faceData)
							.build()))
				.build();

		CardEntity entity = mapper.toImportEntity(model);
		List<CardFaceEntity> faceEntities = mapper.toImportFaces(model.getFaces(), entity);

		assertThat(entity.getUsd()).isEqualByComparingTo("1.2500");
		assertThat(entity.getUsdFoil()).isEqualByComparingTo("2.2500");
		assertThat(entity.getUsdEtched()).isEqualByComparingTo("3.2500");
		assertThat(entity.getEur()).isEqualByComparingTo("1.1500");
		assertThat(entity.getEurFoil()).isEqualByComparingTo("2.1500");
		assertThat(entity.getEurEtched()).isEqualByComparingTo("3.1500");
		assertThat(entity.getTix()).isEqualByComparingTo("0.5000");
		assertThat(entity.getPriceObservedAt()).isEqualTo(model.getPriceObservedAt());
		assertThat(faceEntities).singleElement().satisfies(face -> {
			assertThat(face.getFaceIndex()).isZero();
			assertThat(face.getTypeLine()).isEqualTo("Creature — Wizard");
			assertThat(face.getOracleText()).isEqualTo("Draw a card.");
			assertThat(face.getColors()).isEqualTo(colors);
			assertThat(face.getColorIndicator()).isEqualTo(colorIndicator);
			assertThat(face.getPower()).isEqualTo("1");
			assertThat(face.getToughness()).isEqualTo("2");
			assertThat(face.getLoyalty()).isEqualTo("3");
			assertThat(face.getDefense()).isEqualTo("4");
			assertThat(face.getImageUris()).isEqualTo(imageUris);
			assertThat(face.getFaceData()).isEqualTo(faceData);
			assertThat(face.getCard()).isSameAs(entity);
		});

		Card mappedBack = mapper.toImportModel(entity, faceEntities);
		assertThat(mappedBack.getUsd()).isEqualByComparingTo("1.2500");
		assertThat(mappedBack.getTix()).isEqualByComparingTo("0.5000");
		assertThat(mappedBack.getFaces()).singleElement().satisfies(face -> {
			assertThat(face.getName()).isEqualTo("Front");
			assertThat(face.getColors()).isEqualTo(colors);
			assertThat(face.getImageUris()).isEqualTo(imageUris);
		});
	}

	@Test
	void standardCrudMappingDoesNotReplaceImportOnlyFields() {
		Card model = Card.builder().name("Basic Card").build();

		CardEntity entity = mapper.toEntity(Card.builder().usd(BigDecimal.ONE)
				.faces(List.of(CardFace.builder().name("Front").build())).build());

		assertThat(entity.getUsd()).isNull();
		assertThat(mapper.toImportFaces(model.getFaces(), entity)).isNull();
		assertThat(mapper.toEntity(model).getName()).isEqualTo("Basic Card");
	}

	@Test
	void defaultsMissingFaceManaCostToEmptyString() {
		CardFace face = CardFace.builder().name("Front").build();

		var entity = mapper.toEntity(face);

		assertThat(entity.getManaCost()).isEmpty();
	}

	@Test
	void mapsRulingIdentityAndContentInBothDirections() {
		CardRuling model = CardRuling.builder()
				.oracleId(UUID.randomUUID())
				.publishedAt(LocalDate.parse("2026-10-12"))
				.comment("This card has no additional rules.")
				.source("wotc")
				.build();

		CardRulingEntity entity = mapper.toEntity(model);

		assertThat(mapper.toModel(entity)).isEqualTo(model);
	}
}
