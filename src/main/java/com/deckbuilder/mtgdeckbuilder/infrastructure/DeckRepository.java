package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<DeckEntity, Long> {
	Page<DeckEntity> findByUserId(Long userId, Pageable pageable);
	List<DeckEntity> findByFormatId(Long formatId);

	@Query("""
			SELECT d FROM DeckEntity d
			WHERE (:userId IS NULL OR d.userId = :userId)
			  AND (:formatId IS NULL OR d.formatId = :formatId)
			  AND (:status IS NULL OR d.status = :status)
			""")
	Page<DeckEntity> search(@Param("userId") Long userId, @Param("formatId") Long formatId,
			@Param("status") String status, Pageable pageable);
}