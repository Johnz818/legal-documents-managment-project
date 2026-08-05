package com.example.legal.document.generation;

import com.example.legal.document.CaseDocumentEntity;

import java.util.List;

public record PersistedDocumentGeneration(
        DocumentGenerationEntity generation,
        CaseDocumentEntity caseDocument,
        List<GenerationValueEntity> values
) {

    public PersistedDocumentGeneration {
        values = List.copyOf(values);
    }
}
