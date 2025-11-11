package com.deckbuilder.mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTag {
	private Long cardId;
	private Long tagId;
	private Double weight;
	private Double confidence;
	private String source;
	private String modelVersion;
	private LocalDateTime createdAt;
}
