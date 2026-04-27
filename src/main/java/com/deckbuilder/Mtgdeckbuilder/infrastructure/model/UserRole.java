package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

/**
 * User role enumeration for authorization
 */
public enum UserRole {
    ROLE_USER,
    ROLE_ADMIN
}
