# Clinical RAG Platform

HIPAA-aware Retrieval-Augmented Generation platform for clinical documents. Ingests free-text documents, embeds them into pgvector, retrieves the most relevant chunks for a query, and generates grounded answers with numbered citations.

Built as a portfolio project demonstrating production-grade RAG engineering: interface-driven LLM and embedding providers, a deterministic mock for testing, HNSW vector indexing, and end-to-end integration tests with real Postgres containers.

## What It Does

- **Ingest** — accepts free-text clinical documents via REST, chunks them with overlap, embeds each chunk, and stores chunks + embeddings in PostgreSQL with pgvector
- **Retrieve** — cosine similarity search over HNSW-indexed embeddings, returning the top-K most relevant chunks with source metadata
- **Generate** — builds a citation-grounded prompt and calls a pluggable LLM provider (OpenAI, Bedrock, or a deterministic mock)
- **Deduplicate** — SHA-256 content hashing prevents redundant ingestion
- **Observe** — returns per-query metrics: prompt tokens, completion tokens, latency, retrieved chunk count, and provider names

## Features

| Feature | Status |
|---------|--------|
| Document ingestion | ✅ |
| SHA-256 deduplication | ✅ |
| pgvector embeddings (1536-dim) | ✅ |
| HNSW cosine similarity index | ✅ |
| Top-K retrieval | ✅ |
| Citation-grounded prompt construction | ✅ |
| LLM provider interface + deterministic mock | ✅ |
| REST API for ingest, list, chunks, query | ✅ |
| 15 unit + 10 integration tests | ✅ |
| Docker Compose | ✅ |

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 LTS |
| Framework | Spring Boot 4.1 |
| Vector Store | PostgreSQL 16 + pgvector 0.8 |
| Index Type | HNSW (cosine) |
| Migrations | Flyway |
| Chunking | Custom word-window with overlap |
| Embeddings | Pluggable (mock, OpenAI, Bedrock) |
| LLM | Pluggable (mock, OpenAI, Bedrock) |
| Testing | JUnit 5, Testcontainers, AssertJ |
| Container | Docker, Docker Compose |

## Architecture
