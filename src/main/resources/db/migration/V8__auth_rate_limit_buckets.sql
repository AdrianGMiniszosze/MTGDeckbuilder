-- Shared fixed-window counters for public authentication endpoints.
-- bucket_key is an HMAC, never a raw client address or username.
CREATE TABLE auth_rate_limit_buckets (
    bucket_key          VARCHAR(64) PRIMARY KEY,
    window_start_epoch  BIGINT NOT NULL,
    request_count       BIGINT NOT NULL CHECK (request_count > 0),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_rate_limit_buckets_updated_at
    ON auth_rate_limit_buckets(updated_at);

COMMENT ON TABLE auth_rate_limit_buckets IS
    'Shared per-peer auth rate-limit windows; bucket keys are HMAC-SHA256 values.';
