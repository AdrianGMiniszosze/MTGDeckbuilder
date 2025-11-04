package com.deckbuilder.Mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
@Data
@NoArgsConstructor
public class DeckEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "deck_name", nullable = false)
    private String name;
    
    private String description;
    
    @Column(name = "parent_deck_id")
    private Long parentDeckId;
    
    private String tournament;
    
    @Column(name = "is_private")
    private Boolean isPrivate;
    
    @Column(name = "deck_type")
    private String deckType;
    
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime created;
    
    @Column(name = "format_id", nullable = false)
    private Long formatId;
    
    @Column(name = "last_modification", nullable = false)
    private LocalDateTime modified;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "share_url")
    private String shareUrl;
    
    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardInDeckEntity> cards = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
        modified = created;
    }
    
    @PreUpdate
    protected void onUpdate() {
        modified = LocalDateTime.now();
    }
}