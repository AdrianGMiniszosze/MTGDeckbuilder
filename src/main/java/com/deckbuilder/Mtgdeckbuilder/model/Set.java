package com.deckbuilder.Mtgdeckbuilder.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Set {
    private Long id;
    private String name;
    private String code;
    private String type;
    private String releaseDate;
    private Integer baseSetSize;
    private Integer totalSetSize;
    private Boolean isDigital;
    private Boolean isFoilOnly;
    private Boolean isNonFoilOnly;
}