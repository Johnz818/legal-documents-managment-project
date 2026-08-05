package com.example.legal.document.generation;

import java.util.List;
import java.util.Objects;

public record DocumentGenerationCommand(
        Long caseId,
        Long templateId,
        int versionNumber,
        String timezone,
        String idempotencyKey,
        List<ReviewedGenerationValue> values
) {

    public DocumentGenerationCommand {
        values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
    }
}
