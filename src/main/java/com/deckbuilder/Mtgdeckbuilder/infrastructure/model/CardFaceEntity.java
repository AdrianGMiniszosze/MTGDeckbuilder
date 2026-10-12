package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "card_faces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CardFaceId.class)
public class CardFaceEntity {
	@Id
	@Column(name = "card_id", nullable = false)
	private Long cardId;

	@Id
	@Column(name = "face_index", nullable = false)
	private Short faceIndex;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "card_id", nullable = false, insertable = false, updatable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private CardEntity card;

	@Column(nullable = false)
	private String name;

	@Column(name = "mana_cost", nullable = false)
	private String manaCost = "";

	@Column(name = "type_line")
	private String typeLine;

	@Column(name = "oracle_text", columnDefinition = "TEXT")
	private String oracleText;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private JsonNode colors;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "color_indicator", columnDefinition = "jsonb")
	private JsonNode colorIndicator;

	private String power;
	private String toughness;
	private String loyalty;
	private String defense;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "image_uris", columnDefinition = "jsonb")
	private JsonNode imageUris;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "face_data", nullable = false, columnDefinition = "jsonb")
	private JsonNode faceData;

	@PrePersist
	void assignCardIdIfMissing() {
		if (cardId == null && card != null) {
			cardId = card.getId();
		}
	}
}
