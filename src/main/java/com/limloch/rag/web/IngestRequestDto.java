package com.limloch.rag.web;

import jakarta.validation.constraints.NotBlank;

public record IngestRequestDto(
        @NotBlank String title,
        @NotBlank String content,
        String sourceType,
        String sourceUri
) {}