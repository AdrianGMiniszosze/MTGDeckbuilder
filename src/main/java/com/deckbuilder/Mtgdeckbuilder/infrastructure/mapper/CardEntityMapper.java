package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardFaceEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardRulingEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardFace;
import com.deckbuilder.mtgdeckbuilder.model.CardRuling;
import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardEntityMapper {

	@Mapping(target = "usd", ignore = true)
	@Mapping(target = "usdFoil", ignore = true)
	@Mapping(target = "usdEtched", ignore = true)
	@Mapping(target = "eur", ignore = true)
	@Mapping(target = "eurFoil", ignore = true)
	@Mapping(target = "eurEtched", ignore = true)
	@Mapping(target = "tix", ignore = true)
	@Mapping(target = "priceObservedAt", ignore = true)
	@Named("crudEntity")
	CardEntity toEntity(Card model);

	@Mapping(target = "faces", ignore = true)
	@Named("crudModel")
	Card toModel(CardEntity entity);

	CardEntity toImportEntity(Card model);

	default Card toImportModel(CardEntity entity, List<CardFaceEntity> faces) {
		if (entity == null) {
			return null;
		}
		Card model = toModel(entity);
		model.setFaces(faces == null ? null : faces.stream().map(this::toModel).toList());
		return model;
	}

	default List<CardFaceEntity> toImportFaces(List<CardFace> faces, CardEntity card) {
		if (faces == null) {
			return null;
		}
		return faces.stream().map(face -> {
			CardFaceEntity entity = toEntity(face);
			entity.setCardId(card.getId());
			return entity;
		}).toList();
	}

	@Mapping(target = "cardId", ignore = true)
	CardFaceEntity toEntity(CardFace face);

	CardFace toModel(CardFaceEntity face);

	CardRulingEntity toEntity(CardRuling ruling);

	CardRuling toModel(CardRulingEntity ruling);

	@AfterMapping
	default void applyFaceDefaults(@MappingTarget CardFaceEntity entity) {
		if (entity.getManaCost() == null) {
			entity.setManaCost("");
		}
	}

	@IterableMapping(qualifiedByName = "crudEntity")
	List<CardEntity> toEntityList(List<Card> models);

	@IterableMapping(qualifiedByName = "crudModel")
	List<Card> toModelList(List<CardEntity> entities);
}
