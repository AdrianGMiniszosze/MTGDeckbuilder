package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.SetEntity;

import java.util.Optional;

@Repository
public interface SetRepository extends JpaRepository<SetEntity, Long> {
    Optional<SetEntity> findByName(String name);
}