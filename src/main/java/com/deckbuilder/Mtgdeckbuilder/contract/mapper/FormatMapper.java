package com.deckbuilder.Mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.Mtgdeckbuilder.model.Format;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FormatMapper {
    
    @Mapping(source = "name", target = "format_name")
    @Mapping(source = "maxDeckSize", target = "deck_size")
    FormatDTO toDto(Format format);
    
    @Mapping(target = "name", source = "format_name")
    @Mapping(target = "maxDeckSize", source = "deck_size")
    @Mapping(target = "minDeckSize", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "maxSideboardSize", ignore = true)
    @Mapping(target = "bannedCards", ignore = true)
    @Mapping(target = "restrictedCards", ignore = true)
    Format toModel(FormatDTO dto);
    
    List<FormatDTO> toDtoList(List<Format> formats);
    
    List<Format> toModelList(List<FormatDTO> dtos);
}