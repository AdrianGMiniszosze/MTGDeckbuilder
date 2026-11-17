package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CardInDeckEntityMapper.class})
public interface DeckEntityMapper {

	@Mapping(target = "cards", ignore = true)
	@Mapping(target = "status", source = "status", qualifiedByName = "statusToDb")
	DeckEntity toEntity(Deck model);

	/**
	 * Copies an update onto the already-managed entity. The collection is
	 * ignored on purpose: merging a fresh {@link DeckEntity} whose {@code cards}
	 * list is empty would make {@code orphanRemoval} delete every stored row.
	 * Deck contents are replaced explicitly by {@code DeckServiceImpl}.
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "created", ignore = true)
	@Mapping(target = "cards", ignore = true)
	@Mapping(target = "status", source = "status", qualifiedByName = "statusToDb")
	void updateEntity(Deck model, @MappingTarget DeckEntity entity);

	// MapStruct will map companionCardId automatically as names match.
	// Commanders live in deck_commander and are attached by DeckServiceImpl.
	@Mapping(target = "commanderCardIds", ignore = true)
	@Mapping(target = "status", source = "status", qualifiedByName = "dbToStatus")
	Deck toModel(DeckEntity entity);

	List<DeckEntity> toEntityList(List<Deck> models);

	List<Deck> toModelList(List<DeckEntity> entities);

	@Named("statusToDb")
	default String statusToDb(DeckStatus status) {
		return status != null ? status.getValue() : DeckStatus.DRAFT.getValue();
	}

	@Named("dbToStatus")
	default DeckStatus dbToStatus(String status) {
		return DeckStatus.fromValue(status);
	}
}
