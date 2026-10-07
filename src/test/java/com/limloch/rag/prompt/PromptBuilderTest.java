package com.limloch.rag.prompt;

import com.limloch.rag.repository.ChunkRepository.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    private ScoredChunk chunk(String title, int index, String content) {
        return new ScoredChunk(
                1L,
                UUID.randomUUID(),
                title,
                index,
                content,
                42,
                0.1);
    }

    @Test
    void includesSystemAndUserPrompt() {
        Prompt prompt = builder.build(
                "What are the recommended next steps?",
                List.of(chunk("Cardiology note", 0, "Patient presents with chest pain.")));

        assertThat(prompt.systemPrompt()).contains("clinical decision-support assistant");
        assertThat(prompt.systemPrompt()).contains("[1], [2]");
        assertThat(prompt.userPrompt()).contains("What are the recommended next steps?");
    }

    @Test
    void numbersChunksSequentiallyStartingAtOne() {
        Prompt prompt = builder.build(
                "Any question",
                List.of(
                        chunk("Doc A", 0, "First content."),
                        chunk("Doc B", 0, "Second content."),
                        chunk("Doc C", 0, "Third content.")));

        String user = prompt.userPrompt();
        assertThat(user).contains("[1]");
        assertThat(user).contains("[2]");
        assertThat(user).contains("[3]");
        assertThat(user).doesNotContain("[0]");
        assertThat(user).doesNotContain("[4]");
    }

    @Test
    void handlesEmptyChunkList() {
        Prompt prompt = builder.build("Empty case?", List.of());

        assertThat(prompt.userPrompt()).contains("(none)");
        assertThat(prompt.userPrompt()).contains("Empty case?");
    }

    @Test
    void includesChunkMetadataAndContent() {
        Prompt prompt = builder.build(
                "Question",
                List.of(chunk("Cardiology note", 2, "Troponin elevated.")));

        String user = prompt.userPrompt();
        assertThat(user).contains("Cardiology note");
        assertThat(user).contains("chunk 2");
        assertThat(user).contains("Troponin elevated.");
    }
}