package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import com.deckbuilder.mtgdeckbuilder.model.CardColor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "card_name", nullable = false)
	private String name;

	@Column(name = "mana_cost")  // Now nullable
	private String manaCost;

	@Column(nullable = false)
	private Integer cmc;

	@Column(name = "color_identity")  // Now nullable
	private String colorIdentity;

	@Column(name = "type_line", nullable = false)
	private String typeLine;

	@Column(name = "card_type", nullable = false)
	private String cardType;

	@Column(name = "card_supertype")
	private String cardSupertype;

	@Column(nullable = false)
	private String rarity;

	@Column(name = "card_text", nullable = false, columnDefinition = "TEXT")
	private String cardText;

	@Column(name = "flavor_text", columnDefinition = "TEXT")
	private String flavorText;

	private String power;

	private String toughness;

	@Column(name = "unlimited_copies", nullable = false)
	private Boolean unlimitedCopies = false;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private Boolean foil = false;

	@Column(name = "game_changer", nullable = false)
	private Boolean gameChanger = false;

	@Column(name = "related_card")
	private Long relatedCard;

	@Column(name = "parent_card_id")
	private Long parentCardId;

	@Column(nullable = false)
	private String language;

	@Column(name = "embedding")
	@Lob
	private String embedding;

	private String archetype;

	@Column(name = "card_set")
	private Long cardSet;

	@Column(name = "collector_number")
	private String collectorNumber;

	@Column(name = "promo")
	private Boolean promo = false;

	@Column(name = "variation")
	private Boolean variation = false;

	@ElementCollection
	@CollectionTable(name = "card_colors", joinColumns = @JoinColumn(name = "card_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "color", length = 1)
	private List<CardColor> colors;

	@ElementCollection
	@CollectionTable(name = "card_color_identity", joinColumns = @JoinColumn(name = "card_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "color", length = 1)
	private List<CardColor> colorIdentityColors;

	@ElementCollection
	@CollectionTable(name = "card_types", joinColumns = @JoinColumn(name = "card_id"))
	@Column(name = "type")
	private List<String> types;

	@ElementCollection
	@CollectionTable(name = "card_supertypes", joinColumns = @JoinColumn(name = "card_id"))
	@Column(name = "supertype")
	private List<String> supertypes;

	@ElementCollection
	@CollectionTable(name = "card_keywords", joinColumns = @JoinColumn(name = "card_id"))
	@Column(name = "keyword")
	private List<String> keywords;

	@ElementCollection
	@CollectionTable(name = "card_subtypes", joinColumns = @JoinColumn(name = "card_id"))
	@Column(name = "subtype")
	private List<String> subtypes;
}
