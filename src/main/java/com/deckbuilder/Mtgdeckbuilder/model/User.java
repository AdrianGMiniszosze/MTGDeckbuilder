package com.deckbuilder.mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
	private Long id;
	private String name;
	private String username;
	private String email;
	private String hashedPassword;
	private String country;
	private LocalDateTime registrationDate;
	private Boolean isActive;
	private LocalDateTime lastLogin;
}