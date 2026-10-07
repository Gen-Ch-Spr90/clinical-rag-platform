package com.limloch.rag.web;

import org.springframework.web.client.HttpClientErrorException;
import com.limloch.rag.service.DocumentIngestionService;
import com.limloch.rag.service.IngestionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class QueryControllerTest {

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

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired DocumentIngestionService ingestion;

    @LocalServerPort int port;

    @Test
    @SuppressWarnings("unchecked")
    void returnsAnswerAndSourcesViaHttp() {
        ingestion.ingest(new IngestionRequest(
                "Cardiology note",
                "Patient presents with chest pain radiating to the left arm. "
                        + "EKG shows ST elevation. Troponin elevated. Concern for acute MI.",
                "TEXT",
                null));

        Map<String, Object> body = Map.of(
                "question", "What is the cardiology diagnosis?",
                "topK", 3);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/query",
                body,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("answer")).isNotNull();
        assertThat(response.getBody().get("sources")).isNotNull();
        assertThat(response.getBody().get("metrics")).isNotNull();
    }

    @Test
    void rejectsBlankQuestionViaHttp() {
        Map<String, Object> body = Map.of(
                "question", "",
                "topK", 3);

        try {
            restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/query",
                    body,
                    String.class);
            throw new AssertionError("Expected 400 Bad Request, but request succeeded");
        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest expected) {
            assertThat(expected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }}