package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.SetEntity;
import com.deckbuilder.mtgdeckbuilder.model.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SetEntityMapper {

	SetEntity toEntity(Set model);

	@Mapping(target = "code", ignore = true)
	@Mapping(target = "type", ignore = true)
	@Mapping(target = "releaseDate", ignore = true)
	@Mapping(target = "baseSetSize", ignore = true)
	@Mapping(target = "totalSetSize", ignore = true)
	@Mapping(target = "isDigital", ignore = true)
	@Mapping(target = "isFoilOnly", ignore = true)
	@Mapping(target = "isNonFoilOnly", ignore = true)
	Set toModel(SetEntity entity);

	List<SetEntity> toEntityList(List<Set> models);

	List<Set> toModelList(List<SetEntity> entities);
}
