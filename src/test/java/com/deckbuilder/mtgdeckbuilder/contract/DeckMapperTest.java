package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.CardDeckDTO;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.model.Deck.CardInDeck;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckMapperTest {

	private final DeckMapper mapper = Mappers.getMapper(DeckMapper.class);

	@Test
	void mapsSectionEnumToDatabaseValue() {
		final CardDeckDTO dto = CardDeckDTO.builder()
			.card_id(1)
			.deck_id(2)
			.quantity(3)
			.section(CardDeckDTO.Section.SIDEBOARD)
			.build();

		final CardInDeck card = this.mapper.toCardInDeck(dto);

		assertThat(card.getSection()).isEqualTo("sideboard");
	}

	@Test
	void mapsSectionDatabaseValueToEnum() {
		final CardInDeck card = new CardInDeck(1L, 10L, 2L, 3, "maybeboard");

		final CardDeckDTO dto = this.mapper.toCardDeckDTO(card);

		assertThat(dto.getSection()).isEqualTo(CardDeckDTO.Section.MAYBEBOARD);
	}

	@Test
	void rejectsUnknownSection() {
		final CardInDeck card = new CardInDeck(1L, 10L, 2L, 3, "graveyard");

		assertThatThrownBy(() -> this.mapper.toCardDeckDTO(card))
			.isInstanceOf(InvalidDeckCompositionException.class)
			.hasMessageContaining("Invalid section");
	}

	@Test
	void keepsNullSectionForDefaulting() {
		final CardDeckDTO dto = CardDeckDTO.builder().card_id(1).deck_id(2).quantity(3).build();

		assertThat(this.mapper.toCardInDeck(dto).getSection()).isNull();
	}
}
