package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the real MapStruct conversions. Database rarity values are lowercase
 * while the generated enum constants are uppercase, so these tests exist to
 * stop a regression back to {@code Enum.valueOf}.
 */
@DisplayName("Card Mapper Tests")
class CardMapperTest {

	private final CardMapper mapper = Mappers.getMapper(CardMapper.class);

	@Test
	@DisplayName("Should map the lowercase rarity stored in the database")
	void shouldMapLowercaseRarity() {
		final CardDTO dto = this.mapper.toDto(card("common"));

		assertThat(dto.getRarity()).isEqualTo(CardDTO.Rarity.COMMON);
	}

	@Test
	@DisplayName("Should map Scryfall's bonus rarity")
	void shouldMapBonusRarity() {
		final CardDTO dto = this.mapper.toDto(card("bonus"));

		assertThat(dto.getRarity()).isEqualTo(CardDTO.Rarity.BONUS);
	}

	@Test
	@DisplayName("Should ignore case and surrounding spaces")
	void shouldIgnoreCaseAndSpaces() {
		final CardDTO dto = this.mapper.toDto(card("  RARE  "));

		assertThat(dto.getRarity()).isEqualTo(CardDTO.Rarity.RARE);
	}

	@Test
	@DisplayName("Should keep a null rarity")
	void shouldKeepNullRarity() {
		assertThat(this.mapper.rarityToDto(null)).isNull();
	}

	@Test
	@DisplayName("Should fall back to special for an unknown rarity")
	void shouldFallBackToSpecialForUnknownRarity() {
		assertThat(this.mapper.toDto(card("foil-only")).getRarity()).isEqualTo(CardDTO.Rarity.SPECIAL);
	}

	@Test
	@DisplayName("Should store the lowercase enum value in the database")
	void shouldStoreLowercaseRarity() {
		final Card card = this.mapper.toEntity(dto(CardDTO.Rarity.COMMON));

		assertThat(card.getRarity()).isEqualTo("common");
	}

	@Test
	@DisplayName("Should map a null rarity back to null")
	void shouldMapNullRarityBackToNull() {
		assertThat(this.mapper.rarityToString(null)).isNull();
	}

	private static Card card(String rarity) {
		return Card.builder()
			.name("Ragged Playmate")
			.typeLine("Creature — Rabbit")
			.cardType("Creature")
			.rarity(rarity)
			.cardText("")
			.imageUrl("https://example.com/card.png")
			.language("en")
			.cmc(1)
			.build();
	}

	private static CardDTO dto(CardDTO.Rarity rarity) {
		return CardDTO.builder()
			.card_name("Ragged Playmate")
			.cmc(1)
			.type_line("Creature")
			.card_type("Creature")
			.rarity(rarity)
			.card_text("")
			.image_url("https://example.com/card.png")
			.language("en")
			.build();
	}
}
