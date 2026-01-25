package com.deckbuilder.mtgdeckbuilder.model;

import lombok.Builder;
import lombok.Data;

/**
 * Criteria object for advanced card search functionality
 */
@Data
@Builder
public class CardSearchCriteria {
    private String name;
    private String type;
    private String rarity;
    private String colors;
    private Integer cmcMin;
    private Integer cmcMax;
    private String powerMin;
    private String powerMax;
    private String toughnessMin;
    private String toughnessMax;
    private Long setId;
    private Long formatId;
    private String textContains;
    private String keywords;
    private Boolean isFoil;
    private Boolean isPromo;
    private String language;
    private String sortBy;
    private String sortOrder;

    /**
     * Check if the search has any filtering criteria
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return name != null || type != null || rarity != null || colors != null ||
               cmcMin != null || cmcMax != null || powerMin != null || powerMax != null ||
               toughnessMin != null || toughnessMax != null || setId != null || formatId != null ||
               textContains != null || keywords != null || isFoil != null || isPromo != null ||
               (language != null && !"en".equals(language));
    }
}
