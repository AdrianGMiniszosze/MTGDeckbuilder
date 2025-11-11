package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardEntityMapper {

	CardEntity toEntity(Card model);

	Card toModel(CardEntity entity);

	List<CardEntity> toEntityList(List<Card> models);

	List<Card> toModelList(List<CardEntity> entities);
}
