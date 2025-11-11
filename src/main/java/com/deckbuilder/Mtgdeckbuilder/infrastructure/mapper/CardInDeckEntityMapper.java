package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck.CardInDeck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardInDeckEntityMapper {
	@Mapping(target = "deck", ignore = true)
	CardInDeckEntity toEntity(CardInDeck model);

	CardInDeck toModel(CardInDeckEntity entity);

	List<CardInDeckEntity> toEntityList(List<CardInDeck> models);

	List<CardInDeck> toModelList(List<CardInDeckEntity> entities);
}
