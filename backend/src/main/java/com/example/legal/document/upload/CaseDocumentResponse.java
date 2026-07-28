package com.example.legal.document.upload;

import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;

import java.time.LocalDateTime;

public record CaseDocumentResponse(
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
