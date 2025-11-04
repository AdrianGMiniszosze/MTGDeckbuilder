package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Deck.CardInDeck;
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
