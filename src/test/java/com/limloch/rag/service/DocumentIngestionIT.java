package com.limloch.rag.service;

import com.limloch.rag.embedding.EmbeddingProperties;
import com.limloch.rag.embedding.MockEmbeddingProvider;
import com.limloch.rag.repository.ChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class DocumentIngestionIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("clinicalrag")
            .withUsername("raguser")
            .withPassword("raguser");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired DocumentIngestionService ingestion;
    @Autowired ChunkRepository chunks;
    @Autowired JdbcTemplate jdbc;

    @Test
    void ingestsDocumentAndStoresChunksWithEmbeddings() {
        String content = "Patient presents with fever and cough. ".repeat(200);

        IngestionResult result = ingestion.ingest(
                new IngestionRequest("Test note", content, "TEXT", null));

        assertThat(result.deduplicated()).isFalse();
        assertThat(result.chunkCount()).isGreaterThan(0);

        Integer storedChunks = jdbc.queryForObject(
                "SELECT COUNT(*) FROM chunks WHERE document_id = ?",
                Integer.class, result.documentId());
        assertThat(storedChunks).isEqualTo(result.chunkCount());

        var stored = chunks.findByDocumentId(result.documentId());
        assertThat(stored).hasSize(result.chunkCount());
        assertThat(stored.get(0).embedding()).hasSize(1536);
    }

    @Test
    void deduplicatesIdenticalContent() {
        String content = "Identical content for dedupe test.";

        IngestionResult first = ingestion.ingest(
                new IngestionRequest("Doc A", content, "TEXT", null));
        IngestionResult second = ingestion.ingest(
                new IngestionRequest("Doc B", content, "TEXT", null));

        assertThat(first.deduplicated()).isFalse();
        assertThat(second.deduplicated()).isTrue();
        assertThat(second.documentId()).isEqualTo(first.documentId());
    }

    @Test
    void assignsSequentialChunkIndexes() {
        String content = "The quick brown fox jumps over the lazy dog. ".repeat(300);

        IngestionResult result = ingestion.ingest(
                new IngestionRequest("Sequential test", content, "TEXT", null));

        var indices = jdbc.queryForList(
                "SELECT chunk_index FROM chunks WHERE document_id = ? ORDER BY chunk_index",
                Integer.class, result.documentId());

        assertThat(indices).isNotEmpty();
        for (int i = 0; i < indices.size(); i++) {
            assertThat(indices.get(i)).isEqualTo(i);
        }
    }

    @Test
    void findsMostSimilarChunk() {
        String cardiologyContent =
                "Patient presents with chest pain radiating to the left arm. "
                        + "EKG shows ST elevation. Troponin elevated. Concern for acute MI.";
        String orthoContent =
                "Patient reports right knee pain after a fall. X-ray shows no fracture. "
                        + "Recommend RICE protocol and NSAIDs for pain management.";

        ingestion.ingest(new IngestionRequest("Cardiology note", cardiologyContent, "TEXT", null));
        ingestion.ingest(new IngestionRequest("Ortho note", orthoContent, "TEXT", null));

        MockEmbeddingProvider provider = new MockEmbeddingProvider(new EmbeddingProperties());
        float[] query = provider.embed("chest pain and heart attack");

        var results = chunks.findTopKSimilar(query, 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).distance()).isLessThanOrEqualTo(results.get(1).distance());
        assertThat(results.get(0).documentTitle()).isIn("Cardiology note", "Ortho note");
    }

    @Test
    void returnsEmptyWhenNoChunksExist() {
        MockEmbeddingProvider provider = new MockEmbeddingProvider(new EmbeddingProperties());
        float[] query = provider.embed("anything");

        var results = chunks.findTopKSimilar(query, 5);

        assertThat(results).isEmpty();
    }
}