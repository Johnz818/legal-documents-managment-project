package com.example.legal.document.generation;

import com.example.legal.legalcase.CaseStatus;

import java.util.List;
import java.util.Objects;

public record GenerationPersistenceCommand(
        Long caseId,
        Long templateVersionId,
        CaseStatus caseStatusSnapshot,
        String idempotencyKey,
        String requestSha256,
        String fileName,
        String storageKey,
        String contentType,
        long fileSize,
        List<GenerationValueToPersist> values
) {

    public GenerationPersistenceCommand {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(templateVersionId, "templateVersionId must not be null");
        Objects.requireNonNull(caseStatusSnapshot, "caseStatusSnapshot must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(requestSha256, "requestSha256 must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must not be negative");
        }
    }
}
