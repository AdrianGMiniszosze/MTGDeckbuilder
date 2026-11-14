package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "card_rulings", indexes = @Index(name = "ix_card_rulings_oracle_id", columnList = "oracle_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CardRulingId.class)
public class CardRulingEntity {
	@Id
	@Column(name = "oracle_id", nullable = false)
	private UUID oracleId;

	@Id
	@Column(name = "published_at", nullable = false)
	private LocalDate publishedAt;

	@Id
	@Column(nullable = false, columnDefinition = "TEXT")
	private String comment;

	@Id
	@Column(nullable = false)
	private String source;
}
