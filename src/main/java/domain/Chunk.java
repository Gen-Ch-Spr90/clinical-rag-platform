package com.limloch.rag.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-side DTO — not a JPA entity.
 * The `embedding` column is a PostgreSQL vector(1536) that JPA
 * cannot natively map. Reads and writes go through JdbcTemplate.
 */
public record Chunk(
        long id,
        UUID documentId,
        int chunkIndex,
        String content,
        int tokenCount,
        float[] embedding,
        OffsetDateTime createdAt
) {}