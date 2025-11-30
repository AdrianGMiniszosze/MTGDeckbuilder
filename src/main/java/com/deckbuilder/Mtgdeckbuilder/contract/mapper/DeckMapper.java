package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.CardDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CompleteDeckDTO;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.Deck.CardInDeck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeckMapper {
	@Mapping(source = "name", target = "deck_name")
	@Mapping(source = "parentDeckId", target = "parent_deck_id")
	@Mapping(source = "isPrivate", target = "is_private")
	@Mapping(source = "deckType", target = "deck_type")
	@Mapping(source = "created", target = "creation_date")
	@Mapping(source = "formatId", target = "format")
	@Mapping(source = "modified", target = "last_modification")
	@Mapping(source = "userId", target = "user_id")
	@Mapping(source = "shareUrl", target = "share_url")
	@Mapping(source = "cards", target = "deck_contents")
	DeckDTO toDeckDTO(Deck deck);

	@Mapping(source = "deck_name", target = "name")
	@Mapping(source = "parent_deck_id", target = "parentDeckId")
	@Mapping(source = "is_private", target = "isPrivate")
	@Mapping(source = "deck_type", target = "deckType")
	@Mapping(source = "creation_date", target = "created")
	@Mapping(source = "format", target = "formatId")
	@Mapping(source = "last_modification", target = "modified")
	@Mapping(source = "user_id", target = "userId")
	@Mapping(source = "share_url", target = "shareUrl")
	@Mapping(source = "deck_contents", target = "cards")
	Deck toDeck(DeckDTO deckDTO);

	// Complete deck mappings
	@Mapping(source = "name", target = "deck_name")
	@Mapping(source = "parentDeckId", target = "parent_deck_id")
	@Mapping(source = "isPrivate", target = "is_private")
	@Mapping(source = "created", target = "creation_date")
	@Mapping(source = "formatId", target = "format")
	@Mapping(source = "modified", target = "last_modification")
	@Mapping(source = "userId", target = "user_id")
	@Mapping(source = "shareUrl", target = "share_url")
	@Mapping(target = "main_board", expression = "java(getCardsBySection(deck.getCards(), \"main\"))")
	@Mapping(target = "side_board", expression = "java(getCardsBySection(deck.getCards(), \"sideboard\"))")
	@Mapping(target = "maybe_board", expression = "java(getCardsBySection(deck.getCards(), \"maybeboard\"))")
	CompleteDeckDTO toCompleteDeckDTO(Deck deck);

	List<CompleteDeckDTO> toCompleteDecksDTO(List<Deck> decks);

	@Mapping(source = "cardId", target = "card_id")
	@Mapping(source = "deckId", target = "deck_id")
	CardDeckDTO toCardDeckDTO(CardInDeck cardInDeck);

	@Mapping(target = "id", ignore = true)
	@Mapping(source = "card_id", target = "cardId")
	@Mapping(source = "deck_id", target = "deckId")
	@Mapping(target = "section", ignore = true)
	CardInDeck toCardInDeck(CardDeckDTO cardDeckDTO);

	// Helper method to filter cards by section
	default List<CardDeckDTO> getCardsBySection(List<CardInDeck> cards, String section) {
		if (cards == null) {
			return List.of();
		}
		return cards.stream()
			.filter(card -> section.equals(card.getSection()))
			.map(this::toCardDeckDTO).toList();
	}
}