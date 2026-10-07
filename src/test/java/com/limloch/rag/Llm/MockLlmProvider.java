package com.limloch.rag.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmProviderTest {

    private final MockLlmProvider provider = new MockLlmProvider();

    @Test
    void echoesUserPromptInContent() {
        LlmRequest request = new LlmRequest(
                "You are a helpful assistant.",
                "What is the capital of France?",
                512,
                0.2);

        LlmResponse response = provider.generate(request);

        assertThat(response.content()).contains("What is the capital of France?");
        assertThat(response.content()).startsWith("[MOCK LLM]");
        assertThat(response.provider()).isEqualTo("mock");
    }

    @Test
    void estimatesTokensForBothPrompts() {
        LlmRequest request = new LlmRequest(
                "You are a helpful assistant.",   // 29 chars → ~7 tokens
                "Short question.",                // 15 chars → ~3 tokens
                512,
                0.2);

        LlmResponse response = provider.generate(request);

        assertThat(response.promptTokens()).isGreaterThan(0);
        assertThat(response.completionTokens()).isGreaterThan(0);
        assertThat(response.totalTokens())
                .isEqualTo(response.promptTokens() + response.completionTokens());
    }

    @Test
    void isDeterministic() {
        LlmRequest request = new LlmRequest(
                "System prompt.",
                "User question.",
                512,
                0.2);

        LlmResponse first = provider.generate(request);
        LlmResponse second = provider.generate(request);

        assertThat(first.content()).isEqualTo(second.content());
        assertThat(first.promptTokens()).isEqualTo(second.promptTokens());
        assertThat(first.completionTokens()).isEqualTo(second.completionTokens());
    }
}