package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Card;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CardEntityMapper {
    
    CardEntity toEntity(Card model);
    
    Card toModel(CardEntity entity);
    
    List<CardEntity> toEntityList(List<Card> models);
    
    List<Card> toModelList(List<CardEntity> entities);
}
