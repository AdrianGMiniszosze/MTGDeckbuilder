package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormatRepository extends JpaRepository<FormatEntity, Long> {
	Optional<FormatEntity> findByName(String name);
}