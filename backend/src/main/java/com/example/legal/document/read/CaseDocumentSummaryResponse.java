package com.example.legal.document.read;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;

import java.time.Instant;
import java.time.LocalDateTime;

public record CaseDocumentSummaryResponse(
        Long id,
        Long caseId,
        String originalFileName,
        DocumentSource documentSource,
        DocumentFormat fileFormat,
        String contentType,
        long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant generatedAt
) {
}
