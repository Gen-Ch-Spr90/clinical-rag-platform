package com.limloch.rag.service;

import java.util.UUID;

public record IngestionResult(
        UUID documentId,
        int chunkCount,
        boolean deduplicated
) {}