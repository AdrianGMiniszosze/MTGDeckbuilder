package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

	@Column(name = "min_deck_size")
	private int minDeckSize;

	@Column(name = "max_deck_size")
	private int maxDeckSize;

	@Column(name = "max_sideboard_size")
	private int maxSideboardSize;

	@ElementCollection
	@CollectionTable(name = "format_banned_cards", joinColumns = @JoinColumn(name = "format_id"))
	@Column(name = "card_name")
	private List<String> bannedCards;

	@ElementCollection
	@CollectionTable(name = "format_restricted_cards", joinColumns = @JoinColumn(name = "format_id"))
	@Column(name = "card_name")
	private List<String> restrictedCards;
}