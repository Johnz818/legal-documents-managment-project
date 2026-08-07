package com.example.legal.document.generation;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record GeneratedDocument(
        Long generationId,
        Long caseId,
        Long templateId,
        int versionNumber,
        Long caseDocumentId,
        boolean outputAvailable,
        String fileName,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt
) {
}
