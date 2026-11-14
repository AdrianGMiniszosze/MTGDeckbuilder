package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.DeckIssueDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckValidationDTO;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;

/**
 * Maps the validation domain result to the generated contract DTOs.
 *
 * <p>
 * Enums are mapped by name and the generated enums serialise their lowercase
 * value, so {@code cost} and {@code status} values stay stable in JSON.
 */
@Mapper(componentModel = "spring")
public interface DeckValidationMapper {

	@Mapping(source = "deckId", target = "deck_id")
	@Mapping(source = "validationStatus", target = "validation_status")
	DeckValidationDTO toDto(DeckValidationResult result);

	@Mapping(source = "cardId", target = "card_id")
	@Mapping(source = "deckContentId", target = "deck_content_id")
	@Mapping(source = "section", target = "section", qualifiedByName = "stringToSection")
	DeckIssueDTO toDto(DeckIssue issue);

	/**
	 * MapStruct would use {@code valueOf}, but the generated enum uses lowercase
	 * wire values while the domain stores the same lowercase string.
	 */
	@Named("stringToSection")
	default DeckIssueDTO.Section stringToSection(String section) {
		if (section == null) {
			return null;
		}
		return Arrays.stream(DeckIssueDTO.Section.values())
			.filter(value -> value.getValue().equalsIgnoreCase(section))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown deck section: " + section));
	}
}
