package com.deckbuilder.mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Tag {
	private Long id;
	private String name;
	private String description;
	private String type;
	private String source; // e.g., "user", "system", "ml"
	private Double confidence; // For ML-generated tags
}