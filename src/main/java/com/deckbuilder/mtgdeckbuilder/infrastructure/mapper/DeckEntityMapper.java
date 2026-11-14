package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CardInDeckEntityMapper.class})
public interface DeckEntityMapper {

	@Mapping(target = "cards", ignore = true)
	@Mapping(target = "status", source = "status", qualifiedByName = "statusToDb")
	DeckEntity toEntity(Deck model);

	// MapStruct will map companionCardId automatically as names match
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
