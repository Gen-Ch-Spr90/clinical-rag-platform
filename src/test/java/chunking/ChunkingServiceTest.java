package com.limloch.rag.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkingServiceTest {

    private ChunkingService service(int size, int overlap) {
        ChunkingProperties props = new ChunkingProperties();
        props.setSizeTokens(size);
        props.setOverlapTokens(overlap);
        return new ChunkingService(props);
    }

    @Test
    void splitsLongTextIntoOverlappingChunks() {
        // 25 words, chunk size 10, overlap 3
        String text = "w1 w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12 w13 w14 w15 w16 w17 w18 w19 w20 w21 w22 w23 w24 w25";
        List<String> chunks = service(10, 3).chunk(text);

        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(0).split(" ")).hasSize(10);
        assertThat(chunks.get(1).split(" ")).hasSize(10);
        assertThat(chunks.get(2).split(" ")).hasSize(10);
        assertThat(chunks.get(3).split(" ")).hasSize(4);

    }

    @Test
    void preservesOverlapBetweenConsecutiveChunks() {
        String text = "a b c d e f g h i j k l m n o";
        List<String> chunks = service(10, 3).chunk(text);

        // Last 3 words of chunk 0 should be first 3 words of chunk 1
        String[] first = chunks.get(0).split(" ");
        String[] second = chunks.get(1).split(" ");

        assertThat(second[0]).isEqualTo(first[7]);
        assertThat(second[1]).isEqualTo(first[8]);
        assertThat(second[2]).isEqualTo(first[9]);
    }

    @Test
    void handlesShortTextInSingleChunk() {
        List<String> chunks = service(10, 3).chunk("hello world");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("hello world");
    }

    @Test
    void rejectsInvalidOverlapConfig() {
        assertThatThrownBy(() -> service(10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlapTokens must be less than sizeTokens");
    }

    @Test
    void handlesEmptyAndBlankInput() {
        assertThat(service(10, 3).chunk(null)).isEmpty();
        assertThat(service(10, 3).chunk("")).isEmpty();
        assertThat(service(10, 3).chunk("   \n  \t")).isEmpty();
    }
}