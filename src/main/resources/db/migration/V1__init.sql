-- Enable pgvector for embedding storage
CREATE EXTENSION IF NOT EXISTS vector;

-- Documents: one row per ingested source document
CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(512) NOT NULL,
    source_type     VARCHAR(32)  NOT NULL,
    source_uri      VARCHAR(1024),
    content_hash    VARCHAR(64)  NOT NULL,
    total_chunks    INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    CONSTRAINT uq_documents_hash UNIQUE (content_hash)
);

-- Chunks: one row per text chunk with its embedding
CREATE TABLE chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index     INT  NOT NULL,
    content         TEXT NOT NULL,
    token_count     INT  NOT NULL,
    embedding       vector(1536) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_chunks_doc_index UNIQUE (document_id, chunk_index)
);

-- ivfflat index for cosine similarity search
CREATE INDEX idx_chunks_embedding ON chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_chunks_document ON chunks (document_id, chunk_index);

-- Audit log for ingestion and (later) query operations
CREATE TABLE rag_audit (
    id              BIGSERIAL PRIMARY KEY,
    operation       VARCHAR(32) NOT NULL,
    actor           VARCHAR(128),
    document_id     UUID,
    chunk_count     INT,
    tokens_used     INT,
    latency_ms      BIGINT,
    status          VARCHAR(16) NOT NULL,
    error_message   TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rag_audit_operation ON rag_audit (operation, occurred_at DESC);