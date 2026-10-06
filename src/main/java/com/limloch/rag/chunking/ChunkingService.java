package com.limloch.rag.chunking;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits text into overlapping windows.
 *
 * Token counts are approximated by word counts. Real tokenizers are
 * provider-specific (OpenAI uses tiktoken, Bedrock uses its own). For
 * chunking purposes, word count is close enough to bound chunk size —
 * the goal is a reasonable split, not an exact token budget.
 */
@Service
public class ChunkingService {

    private final int sizeTokens;
    private final int overlapTokens;

    public ChunkingService(ChunkingProperties props) {
        this.sizeTokens = props.getSizeTokens();
        this.overlapTokens = props.getOverlapTokens();

        if (overlapTokens >= sizeTokens) {
            throw new IllegalArgumentException(
                    "overlapTokens must be less than sizeTokens");
        }
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();

        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + sizeTokens, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, start, end)));

            if (end == words.length) break;
            start = end - overlapTokens;
        }

        return chunks;
    }

    /** Approximate token count — used for audit metrics, not for billing. */
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}