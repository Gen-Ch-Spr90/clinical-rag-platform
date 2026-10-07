package com.limloch.rag.prompt;

public record Prompt(
        String systemPrompt,
        String userPrompt
) {}