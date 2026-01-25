package com.deckbuilder.mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Card {
	private Long id;
	private String name;
	private String manaCost;
	private Integer cmc;
	private String colorIdentity;
	private String typeLine;
	private String cardType;
	private String cardSupertype;
	private String rarity;
	private String cardText;
	private String flavorText;
	private String power;
	private String toughness;
	private Boolean unlimitedCopies;
	private String imageUrl;
	private Boolean foil;
	private Boolean gameChanger;
	private Long relatedCard;
	private Long parentCardId;
	private String language;
	private String embedding;
	private String archetype;
	private Long cardSet;
	private String collectorNumber;
	private Boolean promo;
	private Boolean variation;
	private List<String> colors;
	private List<String> colorIdentityColors;
	private List<String> types;           // NEW: Separated card types (Creature, Instant, etc.)
	private List<String> supertypes;      // NEW: Separated supertypes (Legendary, Basic, etc.)
	private List<String> keywords;
	private List<String> subtypes;
}
