package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.DeckIssueDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckValidationDTO;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckValidationMapper;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deck Validation Mapper Tests")
class DeckValidationMapperTest {

	private final DeckValidationMapper mapper = Mappers.getMapper(DeckValidationMapper.class);

	@Test
	@DisplayName("Should map the diagnosis and its snake_case fields")
	void shouldMapDiagnosis() {
		final DeckIssue issue = DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.CARD_BANNED)
				.message("Black Lotus is banned")
				.cardId(10L)
				.deckContentId(20L)
				.section("sideboard")
				.build();
		final DeckValidationResult result = DeckValidationResult.builder()
				.deckId(1L)
				.status(DeckStatus.PUBLISHED)
				.validationStatus(ValidationStatus.ILLEGAL)
				.issues(List.of(issue))
				.build();

		final DeckValidationDTO dto = this.mapper.toDto(result);

		assertThat(dto.getDeck_id()).isEqualTo(1);
		assertThat(dto.getStatus()).isEqualTo(DeckValidationDTO.Status.PUBLISHED);
		assertThat(dto.getValidation_status()).isEqualTo(DeckValidationDTO.Validation_status.ILLEGAL);
		assertThat(dto.getIssues()).hasSize(1);

		final DeckIssueDTO issueDTO = dto.getIssues().getFirst();
		assertThat(issueDTO.getSeverity()).isEqualTo(DeckIssueDTO.Severity.ERROR);
		assertThat(issueDTO.getCode()).isEqualTo("CARD_BANNED");
		assertThat(issueDTO.getMessage()).isEqualTo("Black Lotus is banned");
		assertThat(issueDTO.getCard_id()).isEqualTo(10);
		assertThat(issueDTO.getDeck_content_id()).isEqualTo(20);
		assertThat(issueDTO.getSection()).isEqualTo(DeckIssueDTO.Section.SIDEBOARD);
	}

	@Test
	@DisplayName("Should accept a null section for deck-wide issues")
	void shouldAcceptNullSection() {
		final DeckIssue issue = DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.DECK_TOO_SMALL)
				.message("Too small")
				.build();

		final DeckIssueDTO dto = this.mapper.toDto(issue);

		assertThat(dto.getSection()).isNull();
		assertThat(dto.getCard_id()).isNull();
		assertThat(dto.getDeck_content_id()).isNull();
	}
}
