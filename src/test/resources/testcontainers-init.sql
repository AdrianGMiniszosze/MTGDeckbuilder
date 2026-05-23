-- Initialize pgvector extension before Hibernate creates tables from entities
-- This must run first so the VECTOR type is available if used
CREATE EXTENSION IF NOT EXISTS vector;

