package com.deckbuilder.mtgdeckbuilder.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardFace {
	private Short faceIndex;
	private String name;
	private String manaCost;
	private String typeLine;
	private String oracleText;
	private JsonNode colors;
	private JsonNode colorIndicator;
	private String power;
	private String toughness;
	private String loyalty;
	private String defense;
	private JsonNode imageUris;
	private JsonNode faceData;
}
