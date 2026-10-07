package com.limloch.rag.query;

public record QueryRequest(
        String question,
        Integer topK
) {
    public int effectiveTopK() {
        return topK == null || topK <= 0 ? 5 : Math.min(topK, 20);
    }
}