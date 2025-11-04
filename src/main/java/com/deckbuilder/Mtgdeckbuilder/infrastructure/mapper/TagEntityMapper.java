package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.TagEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TagEntityMapper {
    
    TagEntity toEntity(Tag model);
    
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "confidence", ignore = true)
    Tag toModel(TagEntity entity);
    
    List<TagEntity> toEntityList(List<Tag> models);
    
    List<Tag> toModelList(List<TagEntity> entities);
}
