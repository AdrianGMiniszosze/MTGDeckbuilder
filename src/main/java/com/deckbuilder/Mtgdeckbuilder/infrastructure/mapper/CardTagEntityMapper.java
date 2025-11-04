package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.Mtgdeckbuilder.model.CardTag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardTagEntityMapper {
    
    CardTag toModel(CardTagEntity entity);
    
    CardTagEntity toEntity(CardTag model);
    
    List<CardTag> toModelList(List<CardTagEntity> entities);
    
    List<CardTagEntity> toEntityList(List<CardTag> models);
}
