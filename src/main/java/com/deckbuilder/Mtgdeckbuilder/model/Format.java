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
public class Format {
	private Long id;
	private String name;
	private String description;
	private Integer minDeckSize;
	private Integer maxDeckSize;
	private Integer maxSideboardSize;
	private Integer maxCopies;
	private Boolean singleton;
	private Boolean requiresCommander;
}
