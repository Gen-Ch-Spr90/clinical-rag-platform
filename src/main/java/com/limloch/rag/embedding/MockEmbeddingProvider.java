package com.limloch.rag.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Deterministic embedding for tests and local development.
 * Hashes text to a fixed-dimension unit vector. Same input → same output,
 * every time, in every JVM. No network, no API key, no cost.
 */
@Component
@ConditionalOnProperty(name = "rag.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int dimensions;

    public MockEmbeddingProvider(EmbeddingProperties props) {
        this.dimensions = props.getDimensions();
    }

    @Override
    public float[] embed(String text) {
        long seed = (long) text.hashCode() * 31L + text.length();
        Random rng = new Random(seed);
        float[] vector = new float[dimensions];
        float sumSquares = 0f;
        for (int i = 0; i < dimensions; i++) {
            vector[i] = (float) rng.nextGaussian();
            sumSquares += vector[i] * vector[i];
        }
        float norm = (float) Math.sqrt(sumSquares);
        for (int i = 0; i < dimensions; i++) vector[i] /= norm;
        return vector;
    }

    @Override
    public String name() {
        return "mock";
    }
}