package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardTagEntityMapper {

	CardTag toModel(CardTagEntity entity);

	CardTagEntity toEntity(CardTag model);

	List<CardTag> toModelList(List<CardTagEntity> entities);

	List<CardTagEntity> toEntityList(List<CardTag> models);
}
