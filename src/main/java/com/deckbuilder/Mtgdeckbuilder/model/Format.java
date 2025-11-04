package com.deckbuilder.Mtgdeckbuilder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Format {
    private Long id;
    private String name;
    private String description;
    private Integer minDeckSize;
    private Integer maxDeckSize;
    private Integer maxSideboardSize;
    private List<String> bannedCards;
    private List<String> restrictedCards;
}