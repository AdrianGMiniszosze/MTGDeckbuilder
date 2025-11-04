package com.deckbuilder.Mtgdeckbuilder.model;

/**
 * Represents the five colors in Magic: The Gathering.
 * Uses single-letter codes as used in mana costs and card notation.
 */
public enum CardColor {
    W("White"),
    U("Blue"),
    B("Black"),
    R("Red"),
    G("Green");

    private final String fullName;

    CardColor(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * Get the single-letter code (W, U, B, R, G)
     */
    public String getCode() {
        return this.name();
    }

    /**
     * Parse a color from its single-letter code or full name
     */
    public static CardColor fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        // Try single-letter code first
        try {
            return CardColor.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try full name
            for (CardColor color : values()) {
                if (color.fullName.equalsIgnoreCase(value)) {
                    return color;
                }
            }
            throw new IllegalArgumentException("Unknown MTG color: " + value);
        }
    }
}
