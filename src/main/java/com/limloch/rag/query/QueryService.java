package com.limloch.rag.query;

import com.limloch.rag.embedding.EmbeddingProvider;
import com.limloch.rag.llm.LlmProvider;
import com.limloch.rag.llm.LlmRequest;
import com.limloch.rag.llm.LlmResponse;
import com.limloch.rag.prompt.Prompt;
import com.limloch.rag.prompt.PromptBuilder;
import com.limloch.rag.repository.ChunkRepository;
import com.limloch.rag.repository.ChunkRepository.ScoredChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryService {

    private final EmbeddingProvider embeddings;
    private final ChunkRepository chunks;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llm;

    public QueryService(EmbeddingProvider embeddings,
                        ChunkRepository chunks,
                        PromptBuilder promptBuilder,
                        LlmProvider llm) {
        this.embeddings = embeddings;
        this.chunks = chunks;
        this.promptBuilder = promptBuilder;
        this.llm = llm;
    }

    public QueryResponse query(QueryRequest request) {
        // 1. Embed the question
        float[] queryEmbedding = embeddings.embed(request.question());

        // 2. Retrieve top-K similar chunks
        List<ScoredChunk> retrieved = chunks.findTopKSimilar(
                queryEmbedding, request.effectiveTopK());

        // 3. Build the prompt
        Prompt prompt = promptBuilder.build(request.question(), retrieved);

        // 4. Call the LLM
        LlmResponse llmResponse = llm.generate(new LlmRequest(
                prompt.systemPrompt(),
                prompt.userPrompt(),
                1024,
                0.2));

        // 5. Assemble sources
        List<QueryResponse.Source> sources = new ArrayList<>();
        for (int i = 0; i < retrieved.size(); i++) {
            ScoredChunk c = retrieved.get(i);
            sources.add(new QueryResponse.Source(
                    i + 1,
                    c.documentTitle(),
                    c.chunkIndex(),
                    c.distance()));
        }

        // 6. Assemble metrics
        QueryResponse.Metrics metrics = new QueryResponse.Metrics(
                llmResponse.promptTokens(),
                llmResponse.completionTokens(),
                llmResponse.latencyMs(),
                retrieved.size(),
                embeddings.name(),
                llm.name());

        return new QueryResponse(llmResponse.content(), sources, metrics);
    }
}