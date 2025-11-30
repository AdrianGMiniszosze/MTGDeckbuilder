package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CardMapper {
	@Mapping(source = "name", target = "card_name")
	@Mapping(source = "typeLine", target = "type_line")
	@Mapping(source = "cardType", target = "card_type")
	@Mapping(source = "cardSupertype", target = "card_supertype")
	@Mapping(source = "oracleText", target = "card_text")
	@Mapping(source = "cardSet", target = "card_set")
	@Mapping(source = "imageUrl", target = "image_url")
	@Mapping(source = "manaCost", target = "mana_cost")
	@Mapping(source = "flavorText", target = "flavor_text")
	@Mapping(source = "embedding", target = "embedding")
	@Mapping(source = "relatedCard", target = "related_card")
	@Mapping(source = "power", target = "power")
	@Mapping(source = "toughness", target = "toughness")
	@Mapping(source = "colorIdentity", target = "color_identity")
	@Mapping(source = "language", target = "language")
	@Mapping(source = "rarity", target = "rarity")
	@Mapping(source = "unlimitedCopies", target = "unlimited_copies")
	@Mapping(source = "gameChanger", target = "game_changer")
	@Mapping(source = "collectorNumber", target = "collector_number")
	@Mapping(source = "promo", target = "promo")
	@Mapping(source = "variation", target = "variation")
	@Mapping(target = "card_subtype", ignore = true)
	CardDTO toDto(Card card);

	@Mapping(target = "name", source = "card_name")
	@Mapping(target = "typeLine", source = "type_line")
	@Mapping(target = "cardType", source = "card_type")
	@Mapping(target = "cardSupertype", source = "card_supertype")
	@Mapping(target = "oracleText", source = "card_text")
	@Mapping(target = "cardSet", source = "card_set")
	@Mapping(target = "imageUrl", source = "image_url")
	@Mapping(target = "manaCost", source = "mana_cost")
	@Mapping(target = "flavorText", source = "flavor_text")
	@Mapping(target = "embedding", source = "embedding")
	@Mapping(target = "relatedCard", source = "related_card")
	@Mapping(target = "power", source = "power")
	@Mapping(target = "toughness", source = "toughness")
	@Mapping(target = "colorIdentity", source = "color_identity")
	@Mapping(target = "language", source = "language")
	@Mapping(target = "rarity", source = "rarity")
	@Mapping(target = "unlimitedCopies", source = "unlimited_copies")
	@Mapping(target = "gameChanger", source = "game_changer")
	@Mapping(target = "collectorNumber", source = "collector_number")
	@Mapping(target = "promo", source = "promo")
	@Mapping(target = "variation", source = "variation")
	@Mapping(target = "colors", ignore = true)
	@Mapping(target = "keywords", ignore = true)
	@Mapping(target = "subtypes", ignore = true)
	@Mapping(target = "colorIdentityColors", ignore = true)
	Card toEntity(CardDTO cardDTO);

	// List mapping methods
	java.util.List<CardDTO> toDtoList(java.util.List<Card> cards);
}