package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
@Data
@NoArgsConstructor
public class DeckEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "deck_name", nullable = false)
	private String name;

	private String description;

	@Column(name = "parent_deck_id")
	private Long parentDeckId;

	private String tournament;

	@Column(name = "is_private")
	private Boolean isPrivate = true;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime created;

	@Column(name = "format_id", nullable = false)
	private Long formatId;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime modified;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "share_url")
	private String shareUrl;

	@Column(name = "companion_card_id")
	private Long companionCardId;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CardInDeckEntity> cards = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		this.created = LocalDateTime.now();
		this.modified = this.created;
		if (this.status == null) {
			this.status = "draft";
		}
		// The column is NOT NULL and the contract defaults new decks to private;
		// the mapper cannot apply OpenAPI defaults to Boolean fields.
		if (this.isPrivate == null) {
			this.isPrivate = true;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.modified = LocalDateTime.now();
	}
}