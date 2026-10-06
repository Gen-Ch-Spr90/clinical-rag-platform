package com.limloch.rag.service;

public record IngestionRequest(
        String title,
        String content,
        String sourceType,
        String sourceUri
) {}