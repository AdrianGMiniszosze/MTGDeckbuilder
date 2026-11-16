package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CardMapper {
	Logger LOGGER = LoggerFactory.getLogger(CardMapper.class);

	@Mapping(source = "name", target = "card_name")
	@Mapping(source = "typeLine", target = "type_line")
	@Mapping(source = "cardType", target = "card_type")
	@Mapping(source = "cardSupertype", target = "card_supertype")
	@Mapping(source = "cardText", target = "card_text")
	@Mapping(source = "cardSetId", target = "card_set")
	@Mapping(source = "imageUrl", target = "image_url")
	@Mapping(source = "manaCost", target = "mana_cost")
	@Mapping(source = "flavorText", target = "flavor_text")
	@Mapping(target = "embedding", ignore = true)
	@Mapping(source = "relatedCardId", target = "related_card")
	@Mapping(source = "power", target = "power")
	@Mapping(source = "toughness", target = "toughness")
	@Mapping(source = "colorIdentity", target = "color_identity")
	@Mapping(source = "language", target = "language")
	@Mapping(source = "rarity", target = "rarity", qualifiedByName = "rarityToDto")
	@Mapping(source = "copiesLimit", target = "copies_limit")
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
	@Mapping(target = "cardText", source = "card_text")
	@Mapping(target = "cardSetId", source = "card_set")
	@Mapping(target = "imageUrl", source = "image_url")
	@Mapping(target = "manaCost", source = "mana_cost")
	@Mapping(target = "flavorText", source = "flavor_text")
	@Mapping(target = "relatedCardId", source = "related_card")
	@Mapping(target = "power", source = "power")
	@Mapping(target = "toughness", source = "toughness")
	@Mapping(target = "colorIdentity", source = "color_identity")
	@Mapping(target = "language", source = "language")
	@Mapping(target = "rarity", source = "rarity", qualifiedByName = "rarityToString")
	@Mapping(target = "copiesLimit", source = "copies_limit")
	@Mapping(target = "isBasicLand", ignore = true)
	@Mapping(target = "gameChanger", source = "game_changer")
	@Mapping(target = "collectorNumber", source = "collector_number")
	@Mapping(target = "promo", source = "promo")
	@Mapping(target = "variation", source = "variation")
	@Mapping(target = "parentCardId", ignore = true)  // CardDTO doesn't have parentCardId field
	@Mapping(target = "colors", ignore = true)
	@Mapping(target = "keywords", ignore = true)
	@Mapping(target = "subtypes", ignore = true)
	@Mapping(target = "types", ignore = true)
	@Mapping(target = "supertypes", ignore = true)
	@Mapping(target = "colorIdentityColors", ignore = true)
	@Mapping(target = "scryfallId", ignore = true)
	@Mapping(target = "oracleId", ignore = true)
	@Mapping(target = "importedAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "usd", ignore = true)
	@Mapping(target = "usdFoil", ignore = true)
	@Mapping(target = "usdEtched", ignore = true)
	@Mapping(target = "eur", ignore = true)
	@Mapping(target = "eurFoil", ignore = true)
	@Mapping(target = "eurEtched", ignore = true)
	@Mapping(target = "tix", ignore = true)
	@Mapping(target = "priceObservedAt", ignore = true)
	@Mapping(target = "faces", ignore = true)
	Card toEntity(CardDTO cardDTO);

	// List mapping methods
	java.util.List<CardDTO> toDtoList(java.util.List<Card> cards);

	/**
	 * Converts the rarity stored in the database (Scryfall's lowercase value)
	 * to the contract enum. {@code Enum.valueOf} cannot be used here: database
	 * values are lowercase while the generated constants are uppercase.
	 * Unknown values fall back to {@code special} so one unexpected row does
	 * not break a whole card listing.
	 */
	@Named("rarityToDto")
	default CardDTO.Rarity rarityToDto(String rarity) {
		if (rarity == null) {
			return null;
		}
		final String normalized = rarity.trim().toLowerCase(Locale.ROOT);
		for (final CardDTO.Rarity value : CardDTO.Rarity.values()) {
			if (value.getValue().equals(normalized)) {
				return value;
			}
		}
		LOGGER.warn("Unknown card rarity '{}', falling back to special", rarity);
		return CardDTO.Rarity.SPECIAL;
	}

	/**
	 * Converts the contract enum back to the lowercase value stored in the
	 * database.
	 */
	@Named("rarityToString")
	default String rarityToString(CardDTO.Rarity rarity) {
		return rarity == null ? null : rarity.getValue();
	}
}
