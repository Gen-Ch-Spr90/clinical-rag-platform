# Architecture Decision Records

## ADR-001: pgvector over a dedicated vector database

**Context:** Need to store documents, chunks, and 1536-dim embeddings, then run similarity search.

**Decision:** PostgreSQL 16 + pgvector.

**Rationale:**
- Documents and chunks live in the same transactional store — no distributed transactions
- Flyway already manages Postgres schema
- pgvector has GIST/HNSW indexes and cosine distance operators
- One container to run

**Tradeoffs:** A dedicated vector DB (Pinecone, Weaviate) scales to billions of vectors. For a portfolio project with thousands, pgvector is simpler and faster to operate.

## ADR-002: HNSW over ivfflat for the vector index

**Context:** pgvector offers two index types: ivfflat and HNSW.

**Decision:** HNSW.

**Rationale:**
- ivfflat divides vectors into `lists` clusters and searches only `probes` of them (default 1). With very few rows relative to lists, queries can miss all rows entirely — the classic "index created with little data" warning.
- HNSW is graph-based, has no `lists` parameter, and provides better recall across dataset sizes.
- PostgreSQL's pgvector docs recommend HNSW for most use cases.

**Tradeoffs:** HNSW indexes build more slowly and use more memory. Acceptable for our scale.

## ADR-003: Deterministic mock providers for embedding and LLM

**Context:** Tests should not call external APIs (OpenAI, Bedrock). They should be fast, free, and reproducible.

**Decision:** `MockEmbeddingProvider` and `MockLlmProvider` as the default beans, activated via `rag.embedding.provider=mock` and `rag.llm.provider=mock`.

**Rationale:**
- Mock embeddings hash text to a normalized unit vector — same input, same output, every run
- Mock LLM echoes the prompt — tests can assert on prompt structure
- Real providers are swapped via config without code changes

**Tradeoffs:** The mock does not produce semantically meaningful embeddings. Integration tests verify *plumbing* (routing, ranking order, response shape), not semantic accuracy. Semantic validation requires a real provider.

## ADR-004: Text-bound vector parameters with `CAST(? AS vector)`

**Context:** The pgvector Java client sends `PGvector` objects to the JDBC driver. This works for INSERTs (the column type coerces), but fails silently for SELECT/ORDER BY parameters in pooled Hikari connections — the driver doesn't register the `vector` type, so Postgres can't resolve the operator's parameter type.

**Decision:** Bind the vector as a `text` literal `[0.1,0.2,...]` and cast to `::vector` in SQL.

**Rationale:** `text → vector` is a globally-registered cast. The query resolves reliably regardless of connection type registration.

**Tradeoffs:** Slight overhead from string formatting per query. Negligible compared to network latency.

## ADR-005: Citation-grounded system prompt

**Context:** The LLM must produce answers that reference the retrieved sources, not hallucinate.

**Decision:** The system prompt explicitly states: (1) answer only from the provided context, (2) cite as `[1]`, `[2]`, (3) if context is insufficient, say so.

**Rationale:** Prompt engineering matters as much as retrieval quality. Without an explicit contract, the LLM drifts.

**Tradeoffs:** Overly rigid prompting can reduce answer quality on edge cases. The current prompt biases toward "I don't know" — acceptable for a clinical-support context.

## ADR-006: Mock LLM echoes, not summarizes

**Context:** The mock could produce a fake answer, a summary, or an echo.

**Decision:** Echo with a `[MOCK LLM]` prefix.

**Rationale:** Tests can assert on the exact prompt content by inspecting the response. A summarizing mock would add behavior to test.

**Tradeoffs:** The mock is not a drop-in replacement for demoing the app — but it's designed for tests, not demos.