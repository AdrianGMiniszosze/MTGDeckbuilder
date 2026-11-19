package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.AccountActionTokenEntity;
import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
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
public interface AccountActionTokenRepository extends JpaRepository<AccountActionTokenEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AccountActionTokenEntity> findByTokenHash(String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE AccountActionTokenEntity token
			SET token.consumedAt = :now
			WHERE token.userId = :userId
			  AND token.purpose = :purpose
			  AND token.consumedAt IS NULL
			""")
	int consumeActive(@Param("userId") Long userId, @Param("purpose") AccountActionPurpose purpose,
			@Param("now") LocalDateTime now);

	@Modifying
	@Query("DELETE FROM AccountActionTokenEntity token WHERE token.expiresAt < :cutoff")
	int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
