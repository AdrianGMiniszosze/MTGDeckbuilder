package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.CardDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CompleteDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.IssueCountDTO;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.Deck.CardInDeck;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring", uses = DeckValidationMapper.class)
public interface DeckMapper {
	@Mapping(source = "name", target = "deck_name")
	@Mapping(source = "parentDeckId", target = "parent_deck_id")
	@Mapping(source = "isPrivate", target = "is_private")
	@Mapping(source = "created", target = "creation_date")
	@Mapping(source = "formatId", target = "format")
	@Mapping(source = "modified", target = "last_modification")
	@Mapping(source = "userId", target = "user_id")
	@Mapping(source = "shareUrl", target = "share_url")
	@Mapping(source = "cards", target = "deck_contents")
	@Mapping(target = "validation_status", ignore = true)
	@Mapping(target = "issue_count", ignore = true)
	@Mapping(target = "issues", ignore = true)
	DeckDTO toDeckDTO(Deck deck);

	/**
	 * Single-deck response with the calculated diagnosis attached.
	 */
	@Mapping(source = "deck.name", target = "deck_name")
	@Mapping(source = "deck.parentDeckId", target = "parent_deck_id")
	@Mapping(source = "deck.isPrivate", target = "is_private")
	@Mapping(source = "deck.created", target = "creation_date")
	@Mapping(source = "deck.formatId", target = "format")
	@Mapping(source = "deck.modified", target = "last_modification")
	@Mapping(source = "deck.userId", target = "user_id")
	@Mapping(source = "deck.shareUrl", target = "share_url")
	@Mapping(source = "deck.status", target = "status")
	@Mapping(source = "deck.cards", target = "deck_contents")
	@Mapping(source = "validation.validationStatus", target = "validation_status")
	@Mapping(source = "validation.issues", target = "issues")
	@Mapping(target = "issue_count", expression = "java(issueCount(validation))")
	DeckDTO toDeckDTO(Deck deck, DeckValidationResult validation);

	@Mapping(source = "deck_name", target = "name")
	@Mapping(source = "parent_deck_id", target = "parentDeckId")
	@Mapping(source = "is_private", target = "isPrivate")
	@Mapping(source = "creation_date", target = "created")
	@Mapping(source = "format", target = "formatId")
	@Mapping(source = "last_modification", target = "modified")
	@Mapping(source = "user_id", target = "userId")
	@Mapping(source = "share_url", target = "shareUrl")
	@Mapping(source = "deck_contents", target = "cards")
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "publishedAt", ignore = true)
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
	@Mapping(target = "validation_status", ignore = true)
	@Mapping(target = "issue_count", ignore = true)
	@Mapping(target = "issues", ignore = true)
	@Mapping(target = "main_board", expression = "java(getCardsBySection(deck.getCards(), \"main\"))")
	@Mapping(target = "side_board", expression = "java(getCardsBySection(deck.getCards(), \"sideboard\"))")
	@Mapping(target = "maybe_board", expression = "java(getCardsBySection(deck.getCards(), \"maybeboard\"))")
	CompleteDeckDTO toCompleteDeckDTO(Deck deck);

	/**
	 * Single-deck response with the calculated diagnosis attached.
	 */
	@Mapping(source = "deck.name", target = "deck_name")
	@Mapping(source = "deck.parentDeckId", target = "parent_deck_id")
	@Mapping(source = "deck.isPrivate", target = "is_private")
	@Mapping(source = "deck.created", target = "creation_date")
	@Mapping(source = "deck.formatId", target = "format")
	@Mapping(source = "deck.modified", target = "last_modification")
	@Mapping(source = "deck.userId", target = "user_id")
	@Mapping(source = "deck.shareUrl", target = "share_url")
	@Mapping(source = "deck.status", target = "status")
	@Mapping(source = "validation.validationStatus", target = "validation_status")
	@Mapping(source = "validation.issues", target = "issues")
	@Mapping(target = "issue_count", expression = "java(issueCount(validation))")
	@Mapping(target = "main_board", expression = "java(getCardsBySection(deck.getCards(), \"main\"))")
	@Mapping(target = "side_board", expression = "java(getCardsBySection(deck.getCards(), \"sideboard\"))")
	@Mapping(target = "maybe_board", expression = "java(getCardsBySection(deck.getCards(), \"maybeboard\"))")
	CompleteDeckDTO toCompleteDeckDTO(Deck deck, DeckValidationResult validation);

	/**
	 * Counts issues for the summary. Null validation yields no counts.
	 */
	default IssueCountDTO issueCount(DeckValidationResult validation) {
		if (validation == null) {
			return null;
		}
		return IssueCountDTO.builder().errors(validation.getErrors().size())
			.warnings(validation.getWarnings().size()).build();
	}

	List<CompleteDeckDTO> toCompleteDecksDTO(List<Deck> decks);

	@Mapping(source = "cardId", target = "card_id")
	@Mapping(source = "deckId", target = "deck_id")
	@Mapping(source = "section", target = "section", qualifiedByName = "stringToSection")
	CardDeckDTO toCardDeckDTO(CardInDeck cardInDeck);

	@Mapping(target = "id", ignore = true)
	@Mapping(source = "card_id", target = "cardId")
	@Mapping(source = "deck_id", target = "deckId")
	@Mapping(source = "section", target = "section", qualifiedByName = "sectionToString")
	CardInDeck toCardInDeck(CardDeckDTO cardDeckDTO);

	@Named("sectionToString")
	default String sectionToString(CardDeckDTO.Section section) {
		return section != null ? section.getValue() : null;
	}

	@Named("stringToSection")
	default CardDeckDTO.Section stringToSection(String section) {
		if (section == null) {
			return null;
		}
		return Arrays.stream(CardDeckDTO.Section.values())
			.filter(value -> value.getValue().equalsIgnoreCase(section))
			.findFirst()
			.orElseThrow(() -> new InvalidDeckCompositionException(
					"Invalid section. Must be 'main', 'sideboard', or 'maybeboard'"));
	}

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