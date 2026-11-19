package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.mtgdeckbuilder.model.Format;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FormatMapper {

	@Mapping(source = "name", target = "format_name")
	@Mapping(source = "minDeckSize", target = "min_deck_size")
	@Mapping(source = "maxDeckSize", target = "max_deck_size")
	@Mapping(source = "maxSideboardSize", target = "max_sideboard_size")
	@Mapping(source = "maxCopies", target = "max_copies")
	@Mapping(source = "requiresCommander", target = "requires_commander")
	@Mapping(source = "tracksLegality", target = "tracks_legality")
	@Mapping(target = "deck_size", expression = "java(format.getMaxDeckSize() != null ? format.getMaxDeckSize() : format.getMinDeckSize())")
	FormatDTO toDto(Format format);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "name", source = "format_name")
	@Mapping(target = "minDeckSize", source = "min_deck_size")
	@Mapping(target = "maxDeckSize", source = "max_deck_size")
	@Mapping(target = "maxSideboardSize", source = "max_sideboard_size")
	@Mapping(target = "maxCopies", source = "max_copies")
	@Mapping(target = "requiresCommander", source = "requires_commander")
	@Mapping(target = "tracksLegality", source = "tracks_legality")
	Format toModel(FormatDTO dto);

	List<FormatDTO> toDtoList(List<Format> formats);

	List<Format> toModelList(List<FormatDTO> dtos);
}
