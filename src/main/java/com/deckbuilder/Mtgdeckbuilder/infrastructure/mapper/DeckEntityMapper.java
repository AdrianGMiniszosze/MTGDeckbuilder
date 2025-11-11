package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CardInDeckEntityMapper.class})
public interface DeckEntityMapper {

	@Mapping(target = "cards", ignore = true)
	DeckEntity toEntity(Deck model);

	Deck toModel(DeckEntity entity);

	List<DeckEntity> toEntityList(List<Deck> models);

	List<Deck> toModelList(List<DeckEntity> entities);
}
