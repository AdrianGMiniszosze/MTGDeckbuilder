package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "hashed_password", nullable = false)
	private String hashedPassword;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole role = UserRole.ROLE_USER;

	private String country;

	@Column(name = "registration_date", nullable = false)
	private LocalDateTime registrationDate;

	@PrePersist
	protected void onCreate() {
        this.registrationDate = LocalDateTime.now();
	}

	public enum UserRole {
		ROLE_USER, ROLE_ADMIN
	}
}