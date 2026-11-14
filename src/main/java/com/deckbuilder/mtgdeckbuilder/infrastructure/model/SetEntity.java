package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sets")
@Data
@NoArgsConstructor
public class SetEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(unique = true)
	private String code;

	@Column(name = "released_at")
	private LocalDate releasedAt;

	@Column(name = "set_type")
	private String setType;
}
