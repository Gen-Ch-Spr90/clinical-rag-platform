package com.limloch.rag.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic mock LLM. Echoes the prompt back with a prefix so tests
 * can assert on the structure of the response without calling a real API.
 * Same input → same output. No network, no API key, no cost.
 */
@Component
@ConditionalOnProperty(name = "rag.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public LlmResponse generate(LlmRequest request) {
        String echoed = "[MOCK LLM] " + request.userPrompt();

        // Rough token estimates: ~4 chars per token
        int promptTokens = estimateTokens(request.systemPrompt())
                + estimateTokens(request.userPrompt());
        int completionTokens = estimateTokens(echoed);

        return new LlmResponse(
                echoed,
                promptTokens,
                completionTokens,
                0L,      // instant
                "mock");
    }

    @Override
    public String name() {
        return "mock";
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return Math.max(1, text.length() / 4);
    }
}