package com.limloch.rag.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Real embedding provider backed by OpenAI's /v1/embeddings endpoint.
 * Activated when rag.embedding.provider=openai.
 */
@Component
@ConditionalOnProperty(name = "rag.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final OpenAiProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiEmbeddingProvider(OpenAiProperties props) {
        this.props = props;
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is required when rag.embedding.provider=openai");
        }
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", props.getEmbeddingModel());
            body.put("input", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/embeddings"))
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "OpenAI embeddings failed: HTTP " + response.statusCode()
                                + " - " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode vectorNode = root.get("data").get(0).get("embedding");

            float[] vector = new float[vectorNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) vectorNode.get(i).asDouble();
            }
            return vector;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI embedding interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return "openai:" + props.getEmbeddingModel();
    }
}