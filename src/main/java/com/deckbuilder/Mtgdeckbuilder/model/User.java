package com.deckbuilder.Mtgdeckbuilder.model;

import lombok.*;
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