package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.TagDTO;
import com.deckbuilder.mtgdeckbuilder.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {
	TagDTO toTagDTO(Tag tag);

	List<TagDTO> toTagDTOs(List<Tag> tags);

	@Mapping(target = "description", ignore = true)
	@Mapping(target = "type", ignore = true)
	@Mapping(target = "source", ignore = true)
	@Mapping(target = "confidence", ignore = true)
	Tag toTag(TagDTO tagDTO);

	@Mapping(target = "description", ignore = true)
	@Mapping(target = "type", ignore = true)
	@Mapping(target = "source", ignore = true)
	@Mapping(target = "confidence", ignore = true)
	void updateTag(@MappingTarget Tag tag, TagDTO tagDTO);
}