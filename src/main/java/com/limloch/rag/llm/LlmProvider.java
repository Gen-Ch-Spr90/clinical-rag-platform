package com.limloch.rag.llm;

public interface LlmProvider {

    /** Generate a completion for the given request. */
    LlmResponse generate(LlmRequest request);

    /** Human-readable provider name for audit logs. */
    String name();
}