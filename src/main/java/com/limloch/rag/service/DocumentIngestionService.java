package com.limloch.rag.service;

import com.limloch.rag.chunking.ChunkingService;
import com.limloch.rag.domain.Document;
import com.limloch.rag.embedding.EmbeddingProvider;
import com.limloch.rag.repository.ChunkRepository;
import com.limloch.rag.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final ChunkingService chunker;
    private final EmbeddingProvider embeddings;

    public DocumentIngestionService(DocumentRepository documents,
                                    ChunkRepository chunks,
                                    ChunkingService chunker,
                                    EmbeddingProvider embeddings) {
        this.documents = documents;
        this.chunks = chunks;
        this.chunker = chunker;
        this.embeddings = embeddings;
    }

    @Transactional
    public IngestionResult ingest(IngestionRequest request) {
        String hash = sha256(request.content());

        // Dedupe: same content → return existing document
        var existing = documents.findByContentHash(hash);
        if (existing.isPresent()) {
            Document doc = existing.get();
            return new IngestionResult(doc.getId(), doc.getTotalChunks(), true);
        }

        UUID docId = UUID.randomUUID();
        List<String> chunkTexts = chunker.chunk(request.content());

        List<ChunkRepository.ChunkRow> rows = new ArrayList<>(chunkTexts.size());
        for (int i = 0; i < chunkTexts.size(); i++) {
            String text = chunkTexts.get(i);
            rows.add(new ChunkRepository.ChunkRow(
                    i,
                    text,
                    chunker.estimateTokens(text),
                    embeddings.embed(text)));
        }

        // saveAndFlush ensures the parent row exists before the JdbcTemplate batch
        documents.saveAndFlush(new Document(
                docId,
                request.title(),
                request.sourceType() == null ? "TEXT" : request.sourceType(),
                request.sourceUri(),
                hash,
                rows.size()));

        chunks.batchInsert(docId, rows);

        return new IngestionResult(docId, rows.size(), false);
    }

    private static String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}