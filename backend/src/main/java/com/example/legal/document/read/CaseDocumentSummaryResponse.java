package com.example.legal.document.read;

import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;

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
        LocalDateTime updatedAt
) {
}
