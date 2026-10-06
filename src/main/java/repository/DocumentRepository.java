package com.limloch.rag.repository;

import com.limloch.rag.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByContentHash(String contentHash);
}