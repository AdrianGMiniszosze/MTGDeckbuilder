package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.mtgdeckbuilder.model.Format;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FormatMapper {

	@Mapping(source = "name", target = "format_name")
	@Mapping(target = "deck_size", expression = "java(format.getMaxDeckSize() != null ? format.getMaxDeckSize() : format.getMinDeckSize())")
	FormatDTO toDto(Format format);

	@Mapping(target = "name", source = "format_name")
	@Mapping(target = "maxDeckSize", source = "deck_size")
	@Mapping(target = "minDeckSize", ignore = true)
	@Mapping(target = "description", ignore = true)
	@Mapping(target = "maxSideboardSize", ignore = true)
	@Mapping(target = "maxCopies", ignore = true)
	@Mapping(target = "singleton", ignore = true)
	@Mapping(target = "requiresCommander", ignore = true)
	Format toModel(FormatDTO dto);

	List<FormatDTO> toDtoList(List<Format> formats);

	List<Format> toModelList(List<FormatDTO> dtos);
}