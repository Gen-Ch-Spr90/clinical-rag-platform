package com.limloch.rag.embedding;

import java.util.List;

public interface EmbeddingProvider {

    /** Returns the embedding vector for a single piece of text. */
    float[] embed(String text);

    /** Batched version — implementations may override if the API supports batching. */
    default List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    /** Human-readable name for audit logs. */
    String name();
}