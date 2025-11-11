-- ============================================
-- Vector Search Performance Index
-- ============================================
--
-- This is the ONLY addition your schema needs!
--
-- Purpose: Makes vector similarity searches fast
-- Without this: Slow linear scan on large datasets
-- With this: 10-100x faster using IVFFlat index
--
-- Run this AFTER your 01-create-schema.sql
-- ============================================

-- Enable pgvector extension (if not already enabled)
CREATE EXTENSION IF NOT EXISTS vector;

-- Create IVFFlat index for fast cosine similarity search
-- The 'lists' parameter (100) is good for up to ~100k cards
-- Adjust to 200 if you expect >100k cards
CREATE INDEX IF NOT EXISTS idx_cards_embedding ON cards
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Verify the index was created
-- Run this to check:
-- SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'cards' AND indexname = 'idx_cards_embedding';

-- ============================================
-- That's it! Your schema is complete.
-- ============================================

-- Now you can use fast vector similarity queries like:
--
-- SELECT id, card_name
-- FROM cards
-- WHERE embedding IS NOT NULL
-- ORDER BY embedding <-> (SELECT embedding FROM cards WHERE id = 123)
-- LIMIT 10;

