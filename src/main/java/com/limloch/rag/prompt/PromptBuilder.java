package com.limloch.rag.prompt;

import com.limloch.rag.repository.ChunkRepository.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds RAG prompts from a user question and retrieved chunks.
 *
 * The system prompt establishes the assistant's role and citation contract.
 * The user prompt presents the retrieved context as numbered blocks, followed
 * by the question. The LLM is expected to cite sources as [1], [2], etc.
 *
 * This class is intentionally free of I/O — it's a pure function that
 * transforms input to output, making it trivial to unit test.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are a clinical decision-support assistant.

            Answer the user's question using only the provided context. Do not use
            any knowledge from your training data that is not present in the context.

            If the context does not contain enough information to answer the question,
            respond: "The provided context does not contain enough information to answer this question."

            Cite your sources using bracketed numbers that correspond to the context
            blocks, for example: [1], [2]. Use multiple citations when appropriate.

            Be concise and precise. Prefer quoting the source directly when the answer
            depends on a specific finding or value.""";

    public Prompt build(String question, List<ScoredChunk> chunks) {
        String userPrompt = renderUserPrompt(question, chunks);
        return new Prompt(SYSTEM_PROMPT, userPrompt);
    }

    private static String renderUserPrompt(String question, List<ScoredChunk> chunks) {
        StringBuilder sb = new StringBuilder();

        if (chunks == null || chunks.isEmpty()) {
            sb.append("Context:\n(none)\n\n");
        } else {
            sb.append("Context:\n");
            for (int i = 0; i < chunks.size(); i++) {
                ScoredChunk c = chunks.get(i);
                sb.append('[').append(i + 1).append("] ");
                sb.append('(').append(c.documentTitle())
                        .append(", chunk ").append(c.chunkIndex()).append(")\n");
                sb.append(c.content()).append("\n\n");
            }
        }

        sb.append("Question: ").append(question).append("\n\n");
        sb.append("Answer:");
        return sb.toString();
    }
}