package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	@Modifying
	@Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :cutoff")
	int deleteExpired(@Param("cutoff") LocalDateTime cutoff);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE RefreshTokenEntity t SET t.revokedAt = :now WHERE t.userId = :userId AND t.revokedAt IS NULL")
	int revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
