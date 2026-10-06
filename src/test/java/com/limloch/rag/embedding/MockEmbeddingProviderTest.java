package com.limloch.rag.embedding;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmbeddingProviderTest {

    private final MockEmbeddingProvider provider =
            new MockEmbeddingProvider(new EmbeddingProperties());

    @Test
    void producesCorrectlySizedUnitVectors() {
        float[] vector = provider.embed("patient presents with fever");

        assertThat(vector).hasSize(1536);

        double sumSquares = 0;
        for (float v : vector) sumSquares += v * v;
        assertThat(Math.sqrt(sumSquares)).isCloseTo(1.0, Offset.offset(0.0001));
    }

    @Test
    void isDeterministic() {
        float[] a = provider.embed("same input text");
        float[] b = provider.embed("same input text");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void producesDifferentVectorsForDifferentInputs() {
        float[] a = provider.embed("fever and cough");
        float[] b = provider.embed("headache and nausea");

        assertThat(a).isNotEqualTo(b);
    }
}