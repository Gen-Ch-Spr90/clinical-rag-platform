package com.limloch.rag.llm;

import com.limloch.rag.embedding.OpenAiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Real LLM provider backed by OpenAI's /v1/chat/completions endpoint.
 * Activated when rag.llm.provider=openai.
 */
@Component
@ConditionalOnProperty(name = "rag.llm.provider", havingValue = "openai")
public class OpenAiLlmProvider implements LlmProvider {

    private final OpenAiProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiLlmProvider(OpenAiProperties props) {
        this.props = props;
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is required when rag.llm.provider=openai");
        }
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .build();
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        long startNanos = System.nanoTime();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", props.getLlmModel());

            ArrayNode messages = body.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", request.systemPrompt());

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", request.userPrompt());

            body.put("max_tokens", request.maxTokens());
            body.put("temperature", request.temperature());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "OpenAI chat completion failed: HTTP " + response.statusCode()
                                + " - " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            String content = root.get("choices").get(0).get("message").get("content").asText();
            int promptTokens = root.get("usage").get("prompt_tokens").asInt();
            int completionTokens = root.get("usage").get("completion_tokens").asInt();

            return new LlmResponse(content, promptTokens, completionTokens, latencyMs,
                    "openai:" + props.getLlmModel());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI chat interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return "openai:" + props.getLlmModel();
    }
}