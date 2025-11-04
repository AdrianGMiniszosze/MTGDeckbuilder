package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.TagEntity;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {
    Optional<TagEntity> findByName(String name);
}