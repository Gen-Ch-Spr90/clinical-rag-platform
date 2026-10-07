package com.limloch.rag.web;

public record ChunkResponseDto(
        long id,
        int chunkIndex,
        String content,
        int tokenCount
) {}