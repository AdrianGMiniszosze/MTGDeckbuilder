package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface CardTagMapper {
	Logger LOGGER = LoggerFactory.getLogger(CardTagMapper.class);

	@Mapping(source = "cardId", target = "card_id")
	@Mapping(source = "tagId", target = "tag_id")
	@Mapping(source = "createdAt", target = "created_at")
	@Mapping(source = "modelVersion", target = "model_version")
	@Mapping(source = "source", target = "source", qualifiedByName = "sourceToDto")
	CardTagDTO toCardTagDTO(CardTag cardTag);

	@Mapping(source = "card_id", target = "cardId")
	@Mapping(source = "tag_id", target = "tagId")
	@Mapping(source = "created_at", target = "createdAt")
	@Mapping(source = "model_version", target = "modelVersion")
	@Mapping(source = "source", target = "source", qualifiedByName = "sourceToString")
	CardTag toCardTag(CardTagDTO cardTagDTO);

	List<CardTagDTO> toCardTagDTOs(List<CardTag> cardTags);

	/**
	 * Converts the source stored in the database (lowercase) to the contract
	 * enum. {@code Enum.valueOf} cannot be used here because the database
	 * values and the generated constant names differ in case. Unknown values
	 * fall back to {@code heuristic} so one unexpected row does not break the
	 * whole card.
	 */
	@Named("sourceToDto")
	default CardTagDTO.Source sourceToDto(String source) {
		if (source == null) {
			return null;
		}
		final String normalized = source.trim().toLowerCase(Locale.ROOT);
		for (final CardTagDTO.Source value : CardTagDTO.Source.values()) {
			if (value.getValue().equals(normalized)) {
				return value;
			}
		}
		LOGGER.warn("Unknown card tag source '{}', falling back to heuristic", source);
		return CardTagDTO.Source.HEURISTIC;
	}

	/**
	 * Converts the contract enum back to the lowercase value stored in the
	 * database.
	 */
	@Named("sourceToString")
	default String sourceToString(CardTagDTO.Source source) {
		return source == null ? null : source.getValue();
	}
}
