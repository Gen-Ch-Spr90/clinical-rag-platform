package com.limloch.rag.web;

import com.limloch.rag.domain.Document;
import com.limloch.rag.repository.ChunkRepository;
import com.limloch.rag.repository.DocumentRepository;
import com.limloch.rag.service.DocumentIngestionService;
import com.limloch.rag.service.IngestionRequest;
import com.limloch.rag.service.IngestionResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestion;
    private final DocumentRepository documents;
    private final ChunkRepository chunks;

    public DocumentController(DocumentIngestionService ingestion,
                              DocumentRepository documents,
                              ChunkRepository chunks) {
        this.ingestion = ingestion;
        this.documents = documents;
        this.chunks = chunks;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> ingest(@Valid @RequestBody IngestRequestDto dto) {
        IngestionResult result = ingestion.ingest(new IngestionRequest(
                dto.title(), dto.content(), dto.sourceType(), dto.sourceUri()));

        return Map.of(
                "documentId", result.documentId(),
                "chunkCount", result.chunkCount(),
                "deduplicated", result.deduplicated());
    }

    @GetMapping
    public List<DocumentResponseDto> list() {
        return documents.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentResponseDto get(@PathVariable UUID id) {
        Document doc = documents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return toDto(doc);
    }

    @GetMapping("/{id}/chunks")
    public List<ChunkResponseDto> chunks(@PathVariable UUID id) {
        if (!documents.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return chunks.findByDocumentId(id).stream()
                .map(c -> new ChunkResponseDto(
                        c.id(), c.chunkIndex(), c.content(), c.tokenCount()))
                .toList();
    }

    private DocumentResponseDto toDto(Document doc) {
        return new DocumentResponseDto(
                doc.getId(),
                doc.getTitle(),
                doc.getSourceType(),
                doc.getSourceUri(),
                doc.getTotalChunks(),
                doc.getCreatedAt());
    }
}