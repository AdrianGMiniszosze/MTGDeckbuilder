package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
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
