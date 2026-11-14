package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.CardDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CompleteDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckIssueDTO;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckValidationMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.Deck.CardInDeck;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Deck Mapper Tests")
class DeckMapperTest {

	private final DeckMapper mapper = Mappers.getMapper(DeckMapper.class);

	@BeforeEach
	void wireValidationMapper() {
		ReflectionTestUtils.setField(this.mapper, "deckValidationMapper",
				Mappers.getMapper(DeckValidationMapper.class));
	}

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

	@Test
	@DisplayName("Should attach the diagnosis to a deck response")
	void mapsDiagnosisIntoDeckDTO() {
		final Deck deck = Deck.builder().id(1L).name("Test Deck").userId(2L).formatId(3L)
			.status(DeckStatus.PUBLISHED).build();

		final DeckDTO dto = this.mapper.toDeckDTO(deck, illegalResult());

		assertThat(dto.getStatus()).isEqualTo(DeckDTO.Status.PUBLISHED);
		assertThat(dto.getValidation_status()).isEqualTo(DeckDTO.Validation_status.ILLEGAL);
		assertThat(dto.getIssue_count().getErrors()).isEqualTo(1);
		assertThat(dto.getIssue_count().getWarnings()).isEqualTo(1);
		assertThat(dto.getIssues()).hasSize(2);
		assertThat(dto.getIssues().getFirst().getSection()).isEqualTo(DeckIssueDTO.Section.SIDEBOARD);
	}

	@Test
	@DisplayName("Should attach the diagnosis to a complete deck response")
	void mapsDiagnosisIntoCompleteDeckDTO() {
		final Deck deck = Deck.builder().id(1L).name("Test Deck").userId(2L).formatId(3L)
			.status(DeckStatus.DRAFT)
			.cards(List.of(new CardInDeck(10L, 100L, 1L, 4, "main"),
				new CardInDeck(11L, 101L, 1L, 2, "sideboard")))
			.build();

		final CompleteDeckDTO dto = this.mapper.toCompleteDeckDTO(deck, illegalResult());

		assertThat(dto.getStatus()).isEqualTo(CompleteDeckDTO.Status.DRAFT);
		assertThat(dto.getValidation_status()).isEqualTo(CompleteDeckDTO.Validation_status.ILLEGAL);
		assertThat(dto.getIssue_count().getErrors()).isEqualTo(1);
		assertThat(dto.getMain_board()).hasSize(1);
		assertThat(dto.getSide_board()).hasSize(1);
		assertThat(dto.getIssues()).hasSize(2);
	}

	@Test
	@DisplayName("Should leave the diagnosis empty when listing decks")
	void leavesDiagnosisEmptyOnListMapping() {
		final Deck deck = Deck.builder().id(1L).name("Test Deck").userId(2L).formatId(3L)
			.status(DeckStatus.DRAFT).build();

		final CompleteDeckDTO dto = this.mapper.toCompleteDeckDTO(deck);

		assertThat(dto.getStatus()).isEqualTo(CompleteDeckDTO.Status.DRAFT);
		assertThat(dto.getValidation_status()).isNull();
		assertThat(dto.getIssue_count()).isNull();
		assertThat(dto.getIssues()).isNull();
	}

	private static DeckValidationResult illegalResult() {
		return DeckValidationResult.builder()
			.deckId(1L)
			.status(DeckStatus.DRAFT)
			.validationStatus(ValidationStatus.ILLEGAL)
			.issues(List.of(
				DeckIssue.builder().severity(IssueSeverity.ERROR).code(DeckIssueCode.CARD_BANNED)
					.message("Black Lotus is banned").cardId(10L).deckContentId(20L).section("sideboard")
					.build(),
				DeckIssue.builder().severity(IssueSeverity.WARNING).code(DeckIssueCode.LEGALITY_UNKNOWN)
					.message("Unknown legality").cardId(11L).build()))
			.build();
	}
}
