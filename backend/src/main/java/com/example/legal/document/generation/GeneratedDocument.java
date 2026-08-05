package com.example.legal.document.generation;

import java.time.LocalDateTime;

public record GeneratedDocument(
        Long generationId,
        Long caseId,
        Long templateId,
        int versionNumber,
        Long caseDocumentId,
        boolean outputAvailable,
        String fileName,
        LocalDateTime createdAt
) {
}
