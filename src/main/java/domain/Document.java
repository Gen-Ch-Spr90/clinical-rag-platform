package com.limloch.rag.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_uri", length = 1024)
    private String sourceUri;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "total_chunks", nullable = false)
    private int totalChunks;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Document() {}

    public Document(UUID id, String title, String sourceType, String sourceUri,
                    String contentHash, int totalChunks) {
        this.id = id;
        this.title = title;
        this.sourceType = sourceType;
        this.sourceUri = sourceUri;
        this.contentHash = contentHash;
        this.totalChunks = totalChunks;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getSourceType() { return sourceType; }
    public String getSourceUri() { return sourceUri; }
    public String getContentHash() { return contentHash; }
    public int getTotalChunks() { return totalChunks; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}