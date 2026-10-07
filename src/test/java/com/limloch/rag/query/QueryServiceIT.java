package com.limloch.rag.query;

import com.limloch.rag.service.DocumentIngestionService;
import com.limloch.rag.service.IngestionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class QueryServiceIT {

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

    @Autowired QueryService queryService;
    @Autowired DocumentIngestionService ingestion;

    @Test
    void returnsAnswerWithSourcesAndMetrics() {
        ingestion.ingest(new IngestionRequest(
                "Cardiology note",
                "Patient presents with chest pain radiating to the left arm. "
                        + "EKG shows ST elevation. Troponin elevated. Concern for acute MI.",
                "TEXT",
                null));
        ingestion.ingest(new IngestionRequest(
                "Ortho note",
                "Patient reports right knee pain after a fall. X-ray shows no fracture.",
                "TEXT",
                null));

        QueryResponse response = queryService.query(
                new QueryRequest("What did the cardiology note say?", 3));

        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).isNotEmpty();
        assertThat(response.sources().get(0).citation()).isEqualTo(1);
        assertThat(response.metrics().retrievedChunks()).isGreaterThan(0);
        assertThat(response.metrics().embeddingProvider()).isEqualTo("mock");
        assertThat(response.metrics().llmProvider()).isEqualTo("mock");
    }

    @Test
    void handlesEmptyCorpusGracefully() {
        QueryResponse response = queryService.query(
                new QueryRequest("Any question when there's no data?", 5));

        // LLM mock still echoes, sources empty because nothing was retrieved
        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).isEmpty();
        assertThat(response.metrics().retrievedChunks()).isEqualTo(0);
    }

    @Test
    void capsTopK() {
        // Default topK is 5, cap is 20
        QueryResponse response = queryService.query(
                new QueryRequest("Question", 1000));

        assertThat(response.metrics().retrievedChunks()).isLessThanOrEqualTo(20);
    }
}