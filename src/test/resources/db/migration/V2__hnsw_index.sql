-- Replace ivfflat with HNSW for the embedding index.
--
-- Why: ivfflat divides vectors into `lists` clusters and searches only
-- `probes` of them at query time (default 1). With very few rows
-- relative to lists, a query can miss all rows entirely. HNSW is
-- graph-based, has no `lists` parameter, and offers better recall
-- across dataset sizes. It's the modern default for pgvector.
--
-- The old index was created with lists=100 on an empty table, which
-- triggered pgvector's "ivfflat index created with little data" warning.

DROP INDEX IF EXISTS idx_chunks_embedding;

CREATE INDEX idx_chunks_embedding ON chunks
    USING hnsw (embedding vector_cosine_ops);