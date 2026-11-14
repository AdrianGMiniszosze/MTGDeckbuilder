package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_tag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CardTagId.class)
public class CardTagEntity {

	@Id
	@Column(name = "card_id")
	private Long cardId;

	@Id
	@Column(name = "tag_id")
	private Long tagId;

	@Column(nullable = false)
	private Double weight;

	@Column(nullable = false)
	private Double confidence;

	@Column(nullable = false)
	private String source;

	@Column(name = "model_version")
	private String modelVersion;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
		}
	}
}
