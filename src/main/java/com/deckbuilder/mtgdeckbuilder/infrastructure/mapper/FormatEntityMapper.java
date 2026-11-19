package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.Format;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FormatEntityMapper {

	FormatEntity toEntity(Format model);

	Format toModel(FormatEntity entity);

	/**
	 * Applies a partial format model over the persisted entity. Null fields mean
	 * "keep the stored value", so editing one field never resets the format rules.
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(Format model, @MappingTarget FormatEntity entity);

	List<FormatEntity> toEntityList(List<Format> models);

	List<Format> toModelList(List<FormatEntity> entities);
}
