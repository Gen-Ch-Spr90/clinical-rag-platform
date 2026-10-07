package com.limloch.rag.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponseDto(
        UUID id,
        String title,
        String sourceType,
        String sourceUri,
        int totalChunks,
        OffsetDateTime createdAt
) {}