package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Schema mapping for the JDBC-updated shared rate-limit bucket table.
 */
@Entity
@Table(name = "auth_rate_limit_buckets")
@Getter
@NoArgsConstructor
public class AuthRateLimitBucketEntity {

	@Id
	@Column(name = "bucket_key", length = 64, nullable = false)
	private String bucketKey;

	@Column(name = "window_start_epoch", nullable = false)
	private Long windowStartEpoch;

	@Column(name = "request_count", nullable = false)
	private Long requestCount;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
