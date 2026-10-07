package com.limloch.rag.llm;

public record LlmResponse(
        String content,
        int promptTokens,
        int completionTokens,
        long latencyMs,
        String provider
) {
    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}