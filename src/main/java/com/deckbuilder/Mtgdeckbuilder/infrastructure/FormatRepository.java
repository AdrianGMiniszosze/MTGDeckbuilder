package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.FormatEntity;

import java.util.Optional;

@Repository
public interface FormatRepository extends JpaRepository<FormatEntity, Long> {
    Optional<FormatEntity> findByName(String name);
}