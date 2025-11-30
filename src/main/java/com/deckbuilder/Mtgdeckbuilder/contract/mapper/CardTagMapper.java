package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardTagMapper {

	@Mapping(source = "cardId", target = "card_id")
	@Mapping(source = "tagId", target = "tag_id")
	@Mapping(source = "createdAt", target = "created_at")
	@Mapping(source = "modelVersion", target = "model_version")
	CardTagDTO toCardTagDTO(CardTag cardTag);

	@Mapping(source = "card_id", target = "cardId")
	@Mapping(source = "tag_id", target = "tagId")
	@Mapping(source = "created_at", target = "createdAt")
	@Mapping(source = "model_version", target = "modelVersion")
	CardTag toCardTag(CardTagDTO cardTagDTO);

	List<CardTagDTO> toCardTagDTOs(List<CardTag> cardTags);
}
