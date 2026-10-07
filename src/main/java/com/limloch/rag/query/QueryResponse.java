package com.limloch.rag.query;

import java.util.List;

public record QueryResponse(
        String answer,
        List<Source> sources,
        Metrics metrics
) {
    public record Source(
            int citation,
            String documentTitle,
            int chunkIndex,
            double distance
    ) {}

    public record Metrics(
            int promptTokens,
            int completionTokens,
            long llmLatencyMs,
            int retrievedChunks,
            String embeddingProvider,
            String llmProvider
    ) {}
}