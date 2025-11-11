package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.SetDTO;
import com.deckbuilder.mtgdeckbuilder.model.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SetMapper {
	SetDTO toSetDTO(Set set);

	List<SetDTO> toSetDTOs(List<Set> sets);

	@Mapping(target = "code", ignore = true)
	@Mapping(target = "type", ignore = true)
	@Mapping(target = "releaseDate", ignore = true)
	@Mapping(target = "baseSetSize", ignore = true)
	@Mapping(target = "totalSetSize", ignore = true)
	@Mapping(target = "isDigital", ignore = true)
	@Mapping(target = "isFoilOnly", ignore = true)
	@Mapping(target = "isNonFoilOnly", ignore = true)
	Set toSet(SetDTO setDTO);

	@Mapping(target = "code", ignore = true)
	@Mapping(target = "type", ignore = true)
	@Mapping(target = "releaseDate", ignore = true)
	@Mapping(target = "baseSetSize", ignore = true)
	@Mapping(target = "totalSetSize", ignore = true)
	@Mapping(target = "isDigital", ignore = true)
	@Mapping(target = "isFoilOnly", ignore = true)
	@Mapping(target = "isNonFoilOnly", ignore = true)
	void updateSet(@MappingTarget Set set, SetDTO setDTO);
}