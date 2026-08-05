package com.example.legal.document.generation;

import java.util.List;

public record GenerationPreparation(
        Long caseId,
        Long templateId,
        int versionNumber,
        String timezone,
        List<PreparedGenerationField> fields
) {

    public GenerationPreparation {
        fields = List.copyOf(fields);
    }
}
