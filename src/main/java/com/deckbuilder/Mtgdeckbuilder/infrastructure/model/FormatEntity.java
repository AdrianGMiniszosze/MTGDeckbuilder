package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "formats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FormatEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "format_name", unique = true)
	private String name;

	private String description;

	@Column(name = "min_deck_size", nullable = false)
	private int minDeckSize;

	@Column(name = "max_deck_size")
	private Integer maxDeckSize;

	@Column(name = "max_sideboard_size")
	private Integer maxSideboardSize;

	@Builder.Default
	@Column(name = "max_copies", nullable = false)
	private int maxCopies = 4;

	@Builder.Default
	@Column(nullable = false)
	private boolean singleton = false;

	@Builder.Default
	@Column(name = "requires_commander", nullable = false)
	private boolean requiresCommander = false;
}