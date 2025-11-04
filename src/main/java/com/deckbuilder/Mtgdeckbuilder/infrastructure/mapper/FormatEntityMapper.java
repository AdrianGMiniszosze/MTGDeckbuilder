package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Format;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FormatEntityMapper {
    
    FormatEntity toEntity(Format model);
    
    Format toModel(FormatEntity entity);
    
    List<FormatEntity> toEntityList(List<Format> models);
    
    List<Format> toModelList(List<FormatEntity> entities);
}
